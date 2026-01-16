#include <iostream>
#include <fstream>
#include <vector>
#include <cstring>
#include <cstdint>
#include <algorithm>
#include <chrono>
#include <thread>
#include <future>
#include <zlib.h>

/**
 * Optimized Advanced LLM Codec for SafeTensors compression
 * 
 * Optimizations:
 * 1. Reduced DEFLATE level from 9 to 6 (faster, minimal ratio loss)
 * 2. Parallel block compression using multiple threads
 * 3. Optimized float16 conversion
 * 4. Better memory management
 */

class OptimizedLLMCodec {
private:
    struct Header {
        uint64_t original_size;
        uint64_t json_header_size;
        uint32_t num_floats;
        uint32_t num_blocks;
        uint64_t compressed_tensor_size;
    };

    struct BlockHeader {
        uint64_t compressed_size;
        uint64_t original_size;
    };

    // Optimized float32 to float16 (branchless where possible)
    static uint16_t float32_to_float16(float value) {
        uint32_t f32;
        std::memcpy(&f32, &value, sizeof(float));
        
        uint32_t sign = (f32 >> 16) & 0x8000;
        int32_t exp = ((f32 >> 23) & 0xff) - 127;
        uint32_t mantissa = f32 & 0x7fffff;
        
        if (exp <= -15) return sign;
        if (exp >= 16) return sign | 0x7c00;
        
        exp += 15;
        mantissa >>= 13;
        
        return sign | (exp << 10) | mantissa;
    }

    static float float16_to_float32(uint16_t f16) {
        uint32_t sign = (f16 & 0x8000) << 16;
        int32_t exp = (f16 >> 10) & 0x1f;
        uint32_t mantissa = f16 & 0x3ff;
        
        if (exp == 0) {
            if (mantissa == 0) {
                uint32_t f32 = sign;
                float result;
                std::memcpy(&result, &f32, sizeof(float));
                return result;
            }
            return 0.0f;
        } else if (exp == 31) {
            uint32_t f32 = sign | 0x7f800000 | (mantissa << 13);
            float result;
            std::memcpy(&result, &f32, sizeof(float));
            return result;
        }
        
        exp = exp - 15 + 127;
        uint32_t f32 = sign | (exp << 23) | (mantissa << 13);
        float result;
        std::memcpy(&result, &f32, sizeof(float));
        return result;
    }

    // Delta encoding
    static void delta_encode_inplace(std::vector<uint16_t>& data) {
        if (data.size() <= 1) return;
        
        for (size_t i = data.size() - 1; i > 0; i--) {
            int32_t delta = static_cast<int32_t>(data[i]) - static_cast<int32_t>(data[i-1]);
            data[i] = static_cast<uint16_t>(delta);
        }
    }

    // Delta decoding
    static void delta_decode_inplace(std::vector<uint16_t>& data) {
        if (data.size() <= 1) return;
        
        for (size_t i = 1; i < data.size(); i++) {
            int32_t value = static_cast<int32_t>(data[i-1]) + static_cast<int16_t>(data[i]);
            data[i] = static_cast<uint16_t>(value);
        }
    }

    // Compress a single block (lower compression level for speed)
    static std::vector<uint8_t> compress_block(const uint8_t* data, size_t size) {
        uLongf compressed_size = compressBound(size);
        std::vector<uint8_t> compressed(compressed_size);
        
        // Level 6 instead of 9 - much faster, minimal ratio loss
        int result = compress2(compressed.data(), &compressed_size, data, size, 6);
        
        if (result != Z_OK) {
            std::cerr << "Block compression failed: " << result << std::endl;
            return std::vector<uint8_t>();
        }
        
        compressed.resize(compressed_size);
        return compressed;
    }

    // Decompress a single block
    static std::vector<uint8_t> decompress_block(const uint8_t* data, size_t compressed_size, 
                                                  size_t original_size) {
        std::vector<uint8_t> decompressed(original_size);
        uLongf decompressed_size = original_size;
        
        int result = uncompress(decompressed.data(), &decompressed_size, data, compressed_size);
        
        if (result != Z_OK) {
            std::cerr << "Block decompression failed: " << result << std::endl;
            return std::vector<uint8_t>();
        }
        
        return decompressed;
    }

public:
    static bool compress(const std::string& input_path, const std::string& output_path) {
        auto start = std::chrono::high_resolution_clock::now();
        
        std::ifstream input(input_path, std::ios::binary);
        if (!input) {
            std::cerr << "Cannot open input file: " << input_path << std::endl;
            return false;
        }
        
        input.seekg(0, std::ios::end);
        size_t file_size = input.tellg();
        input.seekg(0, std::ios::beg);
        
        std::cout << "Reading " << file_size << " bytes..." << std::endl;
        
        std::vector<uint8_t> data(file_size);
        input.read(reinterpret_cast<char*>(data.data()), file_size);
        input.close();
        
        if (file_size < 8) {
            std::cerr << "File too small" << std::endl;
            return false;
        }
        
        uint64_t header_size;
        std::memcpy(&header_size, data.data(), sizeof(uint64_t));
        
        if (8 + header_size > file_size) {
            std::cerr << "Invalid header size" << std::endl;
            return false;
        }
        
        std::cout << "JSON header: " << header_size << " bytes" << std::endl;
        
        std::vector<uint8_t> header_data(data.begin(), data.begin() + 8 + header_size);
        std::vector<uint8_t> tensor_data(data.begin() + 8 + header_size, data.end());
        
        // Step 1: Quantization (float32 -> float16)
        size_t num_floats = tensor_data.size() / sizeof(float);
        std::cout << "Quantizing " << num_floats << " floats..." << std::endl;
        
        std::vector<uint16_t> float16_values(num_floats);
        
        // Parallel quantization
        unsigned int num_threads = std::thread::hardware_concurrency();
        if (num_threads == 0) num_threads = 4;
        
        size_t chunk_size = (num_floats + num_threads - 1) / num_threads;
        std::vector<std::future<void>> futures;
        
        for (unsigned int t = 0; t < num_threads; t++) {
            size_t start_idx = t * chunk_size;
            size_t end_idx = std::min(start_idx + chunk_size, num_floats);
            
            if (start_idx >= num_floats) break;
            
            futures.push_back(std::async(std::launch::async, [&, start_idx, end_idx]() {
                for (size_t i = start_idx; i < end_idx; i++) {
                    float value;
                    std::memcpy(&value, tensor_data.data() + i * sizeof(float), sizeof(float));
                    float16_values[i] = float32_to_float16(value);
                }
            }));
        }
        
        for (auto& f : futures) {
            f.wait();
        }
        
        std::cout << "Quantized to " << (float16_values.size() * 2) / (1024.0 * 1024.0) 
                  << " MB" << std::endl;
        
        // Step 2: Delta encoding (in-place for speed)
        // std::cout << "Delta encoding..." << std::endl;
        delta_encode_inplace(float16_values);
        
        // Step 3: Parallel block compression
        //std::cout << "Compressing with " << num_threads << " threads..." << std::endl;
        
        const size_t BLOCK_SIZE = 8 * 1024 * 1024; // 8MB blocks (in uint16_t units)
        size_t num_blocks = (float16_values.size() * sizeof(uint16_t) + BLOCK_SIZE - 1) / BLOCK_SIZE;
        
        std::vector<std::vector<uint8_t>> compressed_blocks(num_blocks);
        std::vector<std::future<void>> compress_futures;
        
        for (size_t b = 0; b < num_blocks; b++) {
            size_t block_start = (b * BLOCK_SIZE) / sizeof(uint16_t);
            size_t block_end = std::min(block_start + BLOCK_SIZE / sizeof(uint16_t), float16_values.size());
            size_t block_size_bytes = (block_end - block_start) * sizeof(uint16_t);
            
            compress_futures.push_back(std::async(std::launch::async, 
                [&, b, block_start, block_size_bytes]() {
                    const uint8_t* block_data = reinterpret_cast<const uint8_t*>(
                        float16_values.data() + block_start);
                    compressed_blocks[b] = compress_block(block_data, block_size_bytes);
                }
            ));
        }
        
        for (auto& f : compress_futures) {
            f.wait();
        }
        
        // Calculate total compressed size
        size_t total_compressed = 0;
        for (const auto& block : compressed_blocks) {
            total_compressed += block.size() + sizeof(BlockHeader);
        }
        
        std::cout << "Compressed to " << total_compressed << " bytes" << std::endl;
        
        // Write output
        std::ofstream output(output_path, std::ios::binary);
        if (!output) {
            std::cerr << "Cannot open output file" << std::endl;
            return false;
        }
        
        Header hdr;
        hdr.original_size = file_size;
        hdr.json_header_size = header_data.size();
        hdr.num_floats = num_floats;
        hdr.num_blocks = num_blocks;
        hdr.compressed_tensor_size = total_compressed;
        
        output.write(reinterpret_cast<const char*>(&hdr), sizeof(Header));
        output.write(reinterpret_cast<const char*>(header_data.data()), header_data.size());
        
        // Write blocks
        for (size_t b = 0; b < num_blocks; b++) {
            size_t block_start = (b * BLOCK_SIZE) / sizeof(uint16_t);
            size_t block_end = std::min(block_start + BLOCK_SIZE / sizeof(uint16_t), float16_values.size());
            size_t original_size = (block_end - block_start) * sizeof(uint16_t);
            
            BlockHeader bhdr;
            bhdr.compressed_size = compressed_blocks[b].size();
            bhdr.original_size = original_size;
            
            output.write(reinterpret_cast<const char*>(&bhdr), sizeof(BlockHeader));
            output.write(reinterpret_cast<const char*>(compressed_blocks[b].data()), 
                        compressed_blocks[b].size());
        }
        
        output.close();
        
        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
        
        size_t output_size = sizeof(Header) + header_data.size() + total_compressed;
        double ratio = static_cast<double>(file_size) / output_size;
        double speed_mbps = (file_size / (1024.0 * 1024.0)) / (duration.count() / 1000.0);
        
        std::cout << "\n=== Compression Results ===" << std::endl;
        std::cout << "Original size:      " << file_size << " bytes (" << file_size / (1024.0 * 1024.0) << " MB)" << std::endl;
        std::cout << "Compressed size:    " << output_size << " bytes (" << output_size / (1024.0 * 1024.0) << " MB)" << std::endl;
        std::cout << "Compression ratio:  " << ratio << ":1" << std::endl;
        std::cout << "Space saved:        " << ((1.0 - 1.0/ratio) * 100.0) << "%" << std::endl;
        std::cout << "Time:               " << duration.count() / 1000.0 << " s" << std::endl;
        std::cout << "Speed:              " << speed_mbps << " MB/s" << std::endl;
        std::cout << "Threads used:       " << num_threads << std::endl;
        
        return true;
    }

    static bool decompress(const std::string& input_path, const std::string& output_path) {
        auto start = std::chrono::high_resolution_clock::now();
        
        std::ifstream input(input_path, std::ios::binary);
        if (!input) {
            std::cerr << "Cannot open input file" << std::endl;
            return false;
        }
        
        Header hdr;
        input.read(reinterpret_cast<char*>(&hdr), sizeof(Header));
        
        std::cout << "Decompressing " << hdr.num_blocks << " blocks..." << std::endl;
        
        std::vector<uint8_t> header_data(hdr.json_header_size);
        input.read(reinterpret_cast<char*>(header_data.data()), hdr.json_header_size);
        
        // Read all blocks
        std::vector<std::pair<std::vector<uint8_t>, size_t>> blocks(hdr.num_blocks);
        
        for (size_t b = 0; b < hdr.num_blocks; b++) {
            BlockHeader bhdr;
            input.read(reinterpret_cast<char*>(&bhdr), sizeof(BlockHeader));
            
            blocks[b].first.resize(bhdr.compressed_size);
            blocks[b].second = bhdr.original_size;
            
            input.read(reinterpret_cast<char*>(blocks[b].first.data()), bhdr.compressed_size);
        }
        input.close();
        
        // Parallel decompression
        std::vector<uint16_t> float16_values(hdr.num_floats);
        std::vector<std::future<void>> futures;
        
        for (size_t b = 0; b < hdr.num_blocks; b++) {
            futures.push_back(std::async(std::launch::async, [&, b]() {
                auto decompressed = decompress_block(blocks[b].first.data(), 
                                                    blocks[b].first.size(),
                                                    blocks[b].second);
                
                const size_t BLOCK_SIZE = 8 * 1024 * 1024;
                size_t block_start = (b * BLOCK_SIZE) / sizeof(uint16_t);
                size_t num_values = decompressed.size() / sizeof(uint16_t);
                
                std::memcpy(float16_values.data() + block_start, decompressed.data(), 
                           decompressed.size());
            }));
        }
        
        for (auto& f : futures) {
            f.wait();
        }
        
        // std::cout << "Delta decoding..." << std::endl;
        delta_decode_inplace(float16_values);
        
        // std::cout << "Converting to float32..." << std::endl;
        std::vector<uint8_t> tensor_data(hdr.num_floats * sizeof(float));
        
        // Parallel dequantization
        unsigned int num_threads = std::thread::hardware_concurrency();
        if (num_threads == 0) num_threads = 4;
        
        size_t chunk_size = (hdr.num_floats + num_threads - 1) / num_threads;
        futures.clear();
        
        for (unsigned int t = 0; t < num_threads; t++) {
            size_t start_idx = t * chunk_size;
            size_t end_idx = std::min(start_idx + chunk_size, static_cast<size_t>(hdr.num_floats));
            
            if (start_idx >= hdr.num_floats) break;
            
            futures.push_back(std::async(std::launch::async, [&, start_idx, end_idx]() {
                for (size_t i = start_idx; i < end_idx; i++) {
                    float value = float16_to_float32(float16_values[i]);
                    std::memcpy(tensor_data.data() + i * sizeof(float), &value, sizeof(float));
                }
            }));
        }
        
        for (auto& f : futures) {
            f.wait();
        }
        
        std::ofstream output(output_path, std::ios::binary);
        if (!output) {
            std::cerr << "Cannot open output file" << std::endl;
            return false;
        }
        
        output.write(reinterpret_cast<const char*>(header_data.data()), header_data.size());
        output.write(reinterpret_cast<const char*>(tensor_data.data()), tensor_data.size());
        output.close();
        
        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
        
        size_t output_size = header_data.size() + tensor_data.size();
        double speed_mbps = (output_size / (1024.0 * 1024.0)) / (duration.count() / 1000.0);
        
        std::cout << "\n=== Decompression Results ===" << std::endl;
        std::cout << "Decompressed size:  " << output_size / (1024.0 * 1024.0) << " MB" << std::endl;
        std::cout << "Time:               " << duration.count() / 1000.0 << " s" << std::endl;
        std::cout << "Speed:              " << speed_mbps << " MB/s" << std::endl;
        
        return true;
    }
};

int main(int argc, char* argv[]) {
    if (argc < 4) {
        std::cout << "Optimized LLM Codec for SafeTensors" << std::endl;
        std::cout << "Usage:" << std::endl;
        std::cout << "  Compress:   " << argv[0] << " -c <input.safetensors> <output.compressed>" << std::endl;
        std::cout << "  Decompress: " << argv[0] << " -d <input.compressed> <output.safetensors>" << std::endl;
        return 1;
    }
    
    std::string mode = argv[1];
    std::string input = argv[2];
    std::string output = argv[3];
    
    if (mode == "-c") {
        if (!OptimizedLLMCodec::compress(input, output)) {
            std::cerr << "Compression failed!" << std::endl;
            return 1;
        }
    } else if (mode == "-d") {
        if (!OptimizedLLMCodec::decompress(input, output)) {
            std::cerr << "Decompression failed!" << std::endl;
            return 1;
        }
    } else {
        std::cerr << "Invalid mode. Use -c or -d" << std::endl;
        return 1;
    }
    
    return 0;
}