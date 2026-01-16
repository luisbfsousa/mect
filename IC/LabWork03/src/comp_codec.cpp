#include <iostream>
#include <fstream>
#include <vector>
#include <cstring>
#include <cstdint>
#include <algorithm>
#include <chrono>
#include <memory>
#include <cmath>
#include <map>
#include <zlib.h>

/**
 * Advanced LLM Codec for SafeTensors compression
 * 
 * Compression Pipeline:
 * 1. Parse SafeTensors format (JSON header + tensor data)
 * 2. Float32 -> Float16 quantization (lossy but acceptable for most LLM weights)
 * 3. Delta encoding to exploit spatial correlation
 * 4. DEFLATE compression (zlib) for optimal compression ratio
 * 
 * This achieves better compression than simple RLE while maintaining
 * reasonable speed and memory usage.
 */

class AdvancedLLMCodec {
private:
    struct Header {
        uint64_t original_size;
        uint64_t json_header_size;
        uint32_t num_floats;
        uint32_t flags;
        uint64_t compressed_tensor_size;
    };

    // IEEE 754 float32 to float16 conversion
    static uint16_t float32_to_float16(float value) {
        uint32_t f32;
        std::memcpy(&f32, &value, sizeof(float));
        
        uint32_t sign = (f32 >> 16) & 0x8000;
        int32_t exp = ((f32 >> 23) & 0xff) - 127;
        uint32_t mantissa = f32 & 0x7fffff;
        
        // Handle special cases
        if (exp <= -15) {
            // Zero or subnormal -> round to zero
            return sign;
        } else if (exp >= 16) {
            // Infinity or overflow
            return sign | 0x7c00;
        }
        
        // Normalized number
        exp += 15;
        mantissa >>= 13;
        
        return sign | (exp << 10) | mantissa;
    }

    // Float16 to float32 conversion
    static float float16_to_float32(uint16_t f16) {
        uint32_t sign = (f16 & 0x8000) << 16;
        int32_t exp = (f16 >> 10) & 0x1f;
        uint32_t mantissa = f16 & 0x3ff;
        
        if (exp == 0) {
            if (mantissa == 0) {
                // Zero
                uint32_t f32 = sign;
                float result;
                std::memcpy(&result, &f32, sizeof(float));
                return result;
            }
            // Subnormal - not handling for simplicity
            return 0.0f;
        } else if (exp == 31) {
            // Infinity or NaN
            uint32_t f32 = sign | 0x7f800000 | (mantissa << 13);
            float result;
            std::memcpy(&result, &f32, sizeof(float));
            return result;
        }
        
        // Normalized
        exp = exp - 15 + 127;
        uint32_t f32 = sign | (exp << 23) | (mantissa << 13);
        float result;
        std::memcpy(&result, &f32, sizeof(float));
        return result;
    }

    // Delta encoding - exploits temporal/spatial correlation in weights
    static std::vector<int16_t> delta_encode(const std::vector<uint16_t>& data) {
        std::vector<int16_t> deltas;
        deltas.reserve(data.size());
        
        if (data.empty()) return deltas;
        
        // Store first value as-is
        deltas.push_back(static_cast<int16_t>(data[0]));
        
        // Store differences
        for (size_t i = 1; i < data.size(); i++) {
            int32_t delta = static_cast<int32_t>(data[i]) - static_cast<int32_t>(data[i-1]);
            deltas.push_back(static_cast<int16_t>(std::clamp(delta, -32768, 32767)));
        }
        
        return deltas;
    }

    // Delta decoding
    static std::vector<uint16_t> delta_decode(const std::vector<int16_t>& deltas) {
        std::vector<uint16_t> data;
        data.reserve(deltas.size());
        
        if (deltas.empty()) return data;
        
        data.push_back(static_cast<uint16_t>(deltas[0]));
        
        for (size_t i = 1; i < deltas.size(); i++) {
            int32_t value = static_cast<int32_t>(data[i-1]) + static_cast<int32_t>(deltas[i]);
            data.push_back(static_cast<uint16_t>(std::clamp(value, 0, 65535)));
        }
        
        return data;
    }

    // DEFLATE compression using zlib (much better than RLE)
    static std::vector<uint8_t> deflate_compress(const uint8_t* data, size_t size) {
        // Estimate output size (worst case: slightly larger than input)
        uLongf compressed_size = compressBound(size);
        std::vector<uint8_t> compressed(compressed_size);
        
        // Use maximum compression level (9) for best ratio
        int result = compress2(compressed.data(), &compressed_size, 
                              data, size, 9);
        
        if (result != Z_OK) {
            std::cerr << "DEFLATE compression failed: " << result << std::endl;
            return std::vector<uint8_t>();
        }
        
        compressed.resize(compressed_size);
        return compressed;
    }

    // DEFLATE decompression
    static std::vector<uint8_t> deflate_decompress(const uint8_t* data, size_t compressed_size, 
                                                    size_t original_size) {
        std::vector<uint8_t> decompressed(original_size);
        uLongf decompressed_size = original_size;
        
        int result = uncompress(decompressed.data(), &decompressed_size,
                               data, compressed_size);
        
        if (result != Z_OK) {
            std::cerr << "DEFLATE decompression failed: " << result << std::endl;
            return std::vector<uint8_t>();
        }
        
        decompressed.resize(decompressed_size);
        return decompressed;
    }

public:
    static bool compress(const std::string& input_path, const std::string& output_path) {
        auto start = std::chrono::high_resolution_clock::now();
        
        // Read input file
        std::ifstream input(input_path, std::ios::binary);
        if (!input) {
            std::cerr << "Cannot open input file: " << input_path << std::endl;
            return false;
        }
        
        // Get file size
        input.seekg(0, std::ios::end);
        size_t file_size = input.tellg();
        input.seekg(0, std::ios::beg);
        
        std::cout << "Reading " << file_size << " bytes (" 
                  << file_size / (1024.0 * 1024.0) << " MB)..." << std::endl;
        
        // Read entire file
        std::vector<uint8_t> data(file_size);
        input.read(reinterpret_cast<char*>(data.data()), file_size);
        input.close();
        
        // Parse SafeTensors header
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
        
        std::cout << "JSON header size: " << header_size << " bytes" << std::endl;
        
        // Separate header and tensor data
        std::vector<uint8_t> header_data(data.begin(), data.begin() + 8 + header_size);
        std::vector<uint8_t> tensor_data(data.begin() + 8 + header_size, data.end());
        
        std::cout << "Tensor data size: " << tensor_data.size() << " bytes" << std::endl;
        
        // Step 1: Quantization (float32 -> float16)
        size_t num_floats = tensor_data.size() / sizeof(float);
        std::cout << "Converting " << num_floats << " floats to float16..." << std::endl;
        
        std::vector<uint16_t> float16_values;
        float16_values.reserve(num_floats);
        
        for (size_t i = 0; i < num_floats; i++) {
            float value;
            std::memcpy(&value, tensor_data.data() + i * sizeof(float), sizeof(float));
            float16_values.push_back(float32_to_float16(value));
        }
        
        std::cout << "Quantized to " << (float16_values.size() * 2) / (1024.0 * 1024.0) 
                  << " MB (50% reduction)" << std::endl;
        
        // Step 2: Delta encoding
        std::cout << "Applying delta encoding..." << std::endl;
        auto deltas = delta_encode(float16_values);
        
        // Convert deltas to bytes
        std::vector<uint8_t> delta_bytes(deltas.size() * sizeof(int16_t));
        std::memcpy(delta_bytes.data(), deltas.data(), delta_bytes.size());
        
        // Step 3: DEFLATE compression
        std::cout << "Applying DEFLATE compression..." << std::endl;
        auto compressed_tensor = deflate_compress(delta_bytes.data(), delta_bytes.size());
        
        if (compressed_tensor.empty()) {
            std::cerr << "Compression failed!" << std::endl;
            return false;
        }
        
        std::cout << "Compressed tensor data: " << compressed_tensor.size() << " bytes ("
                  << compressed_tensor.size() / (1024.0 * 1024.0) << " MB)" << std::endl;
        
        // Write output file
        std::ofstream output(output_path, std::ios::binary);
        if (!output) {
            std::cerr << "Cannot open output file: " << output_path << std::endl;
            return false;
        }
        
        // Write custom header
        Header hdr;
        hdr.original_size = file_size;
        hdr.json_header_size = header_data.size();
        hdr.num_floats = num_floats;
        hdr.flags = 0;
        hdr.compressed_tensor_size = compressed_tensor.size();
        
        output.write(reinterpret_cast<const char*>(&hdr), sizeof(Header));
        
        // Write JSON header (uncompressed - it's already small)
        output.write(reinterpret_cast<const char*>(header_data.data()), header_data.size());
        
        // Write compressed tensor data
        output.write(reinterpret_cast<const char*>(compressed_tensor.data()), 
                    compressed_tensor.size());
        
        output.close();
        
        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
        
        size_t output_size = sizeof(Header) + header_data.size() + compressed_tensor.size();
        double ratio = static_cast<double>(file_size) / output_size;
        double speed_mbps = (file_size / (1024.0 * 1024.0)) / (duration.count() / 1000.0);
        
        std::cout << "\n=== Compression Results ===" << std::endl;
        std::cout << "Original size:      " << file_size << " bytes ("
                  << file_size / (1024.0 * 1024.0) << " MB)" << std::endl;
        std::cout << "Compressed size:    " << output_size << " bytes ("
                  << output_size / (1024.0 * 1024.0) << " MB)" << std::endl;
        std::cout << "Compression ratio:  " << ratio << ":1" << std::endl;
        std::cout << "Space saved:        " << ((1.0 - 1.0/ratio) * 100.0) << "%" << std::endl;
        std::cout << "Time:               " << duration.count() << " ms ("
                  << duration.count() / 1000.0 << " s)" << std::endl;
        std::cout << "Speed:              " << speed_mbps << " MB/s" << std::endl;
        
        return true;
    }

    static bool decompress(const std::string& input_path, const std::string& output_path) {
        auto start = std::chrono::high_resolution_clock::now();
        
        std::ifstream input(input_path, std::ios::binary);
        if (!input) {
            std::cerr << "Cannot open input file: " << input_path << std::endl;
            return false;
        }
        
        // Read header
        Header hdr;
        input.read(reinterpret_cast<char*>(&hdr), sizeof(Header));
        
        std::cout << "Reading compressed file..." << std::endl;
        std::cout << "Original size: " << hdr.original_size << " bytes" << std::endl;
        std::cout << "Number of floats: " << hdr.num_floats << std::endl;
        
        // Read JSON header
        std::vector<uint8_t> header_data(hdr.json_header_size);
        input.read(reinterpret_cast<char*>(header_data.data()), hdr.json_header_size);
        
        // Read compressed tensor data
        std::vector<uint8_t> compressed_tensor(hdr.compressed_tensor_size);
        input.read(reinterpret_cast<char*>(compressed_tensor.data()), 
                  hdr.compressed_tensor_size);
        input.close();
        
        std::cout << "Decompressing " << compressed_tensor.size() << " bytes..." << std::endl;
        
        // Step 1: DEFLATE decompress
        size_t expected_delta_size = hdr.num_floats * sizeof(int16_t);
        auto delta_bytes = deflate_decompress(compressed_tensor.data(), 
                                              compressed_tensor.size(),
                                              expected_delta_size);
        
        if (delta_bytes.empty()) {
            std::cerr << "Decompression failed!" << std::endl;
            return false;
        }
        
        std::cout << "Decompressed to " << delta_bytes.size() << " bytes" << std::endl;
        
        // Step 2: Convert bytes to deltas
        std::vector<int16_t> deltas(hdr.num_floats);
        std::memcpy(deltas.data(), delta_bytes.data(), 
                   std::min(delta_bytes.size(), deltas.size() * sizeof(int16_t)));
        
        // Step 3: Delta decode
        std::cout << "Applying delta decoding..." << std::endl;
        auto float16_values = delta_decode(deltas);
        
        // Step 4: Dequantization (float16 -> float32)
        std::cout << "Converting back to float32..." << std::endl;
        std::vector<uint8_t> tensor_data(hdr.num_floats * sizeof(float));
        
        for (size_t i = 0; i < hdr.num_floats; i++) {
            float value = float16_to_float32(float16_values[i]);
            std::memcpy(tensor_data.data() + i * sizeof(float), &value, sizeof(float));
        }
        
        // Write output file
        std::ofstream output(output_path, std::ios::binary);
        if (!output) {
            std::cerr << "Cannot open output file: " << output_path << std::endl;
            return false;
        }
        
        // Reconstruct SafeTensors format
        output.write(reinterpret_cast<const char*>(header_data.data()), header_data.size());
        output.write(reinterpret_cast<const char*>(tensor_data.data()), tensor_data.size());
        output.close();
        
        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
        
        size_t output_size = header_data.size() + tensor_data.size();
        double speed_mbps = (output_size / (1024.0 * 1024.0)) / (duration.count() / 1000.0);
        
        std::cout << "\n=== Decompression Results ===" << std::endl;
        std::cout << "Decompressed size:  " << output_size << " bytes ("
                  << output_size / (1024.0 * 1024.0) << " MB)" << std::endl;
        std::cout << "Time:               " << duration.count() << " ms ("
                  << duration.count() / 1000.0 << " s)" << std::endl;
        std::cout << "Speed:              " << speed_mbps << " MB/s" << std::endl;
        
        return true;
    }
};

int main(int argc, char* argv[]) {
    if (argc < 4) {
        std::cout << "Advanced LLM Codec for SafeTensors Compression" << std::endl;
        std::cout << "Usage:" << std::endl;
        std::cout << "  Compress:   " << argv[0] << " -c <input.safetensors> <output.compressed>" << std::endl;
        std::cout << "  Decompress: " << argv[0] << " -d <input.compressed> <output.safetensors>" << std::endl;
        return 1;
    }
    
    std::string mode = argv[1];
    std::string input = argv[2];
    std::string output = argv[3];
    
    if (mode == "-c") {
        if (!AdvancedLLMCodec::compress(input, output)) {
            std::cerr << "Compression failed!" << std::endl;
            return 1;
        }
    } else if (mode == "-d") {
        if (!AdvancedLLMCodec::decompress(input, output)) {
            std::cerr << "Decompression failed!" << std::endl;
            return 1;
        }
    } else {
        std::cerr << "Invalid mode: " << mode << std::endl;
        std::cerr << "Use -c for compression or -d for decompression" << std::endl;
        return 1;
    }
    
    return 0;
}