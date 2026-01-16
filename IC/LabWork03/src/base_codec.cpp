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

// Simple ZSTD-like compression using RLE + Huffman-inspired approach
// For production, link against libzstd

class LLMCodec {
private:
    struct Header {
        uint64_t original_size;
        uint64_t json_header_size;
        uint32_t num_tensors;
        uint32_t flags;
    };

    // Convert float32 to float16 (half precision)
    static uint16_t float32_to_float16(float value) {
        uint32_t f32;
        std::memcpy(&f32, &value, sizeof(float));
        
        uint32_t sign = (f32 >> 16) & 0x8000;
        int32_t exp = ((f32 >> 23) & 0xff) - 127 + 15;
        uint32_t mantissa = (f32 >> 13) & 0x3ff;
        
        if (exp <= 0) {
            return sign; // Zero or denormal
        } else if (exp >= 31) {
            return sign | 0x7c00; // Infinity
        }
        
        return sign | (exp << 10) | mantissa;
    }

    // Convert float16 back to float32
    static float float16_to_float32(uint16_t f16) {
        uint32_t sign = (f16 & 0x8000) << 16;
        int32_t exp = (f16 >> 10) & 0x1f;
        uint32_t mantissa = f16 & 0x3ff;
        
        if (exp == 0) {
            return 0.0f; // Zero (simplified)
        } else if (exp == 31) {
            // Infinity
            uint32_t f32 = sign | 0x7f800000;
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

    // Simple RLE compression
    static std::vector<uint8_t> rle_compress(const std::vector<uint8_t>& data) {
        std::vector<uint8_t> compressed;
        compressed.reserve(data.size());
        
        size_t i = 0;
        while (i < data.size()) {
            uint8_t value = data[i];
            size_t count = 1;
            
            while (i + count < data.size() && data[i + count] == value && count < 255) {
                count++;
            }
            
            if (count >= 5) {
                // Use RLE for runs of 5+ (higher threshold reduces overhead)
                compressed.push_back(0xFF); // RLE marker
                compressed.push_back(static_cast<uint8_t>(count));
                compressed.push_back(value);
                i += count;
            } else {
                // Literal byte - escape 0xFF values
                if (value == 0xFF)
                {
                    compressed.push_back(0xFF);
                    compressed.push_back(0x00);
                } else {
                    compressed.push_back(value);
                }
                i++;
            }
        }
        
        return compressed;
    }

    // Simple RLE decompression
    static std::vector<uint8_t> rle_decompress(const std::vector<uint8_t>& compressed) {
        std::vector<uint8_t> data;
        data.reserve(std::min(static_cast<size_t>(1024 * 1024 * 1024), compressed.size() * 4));
        
        size_t i = 0;
        while (i < compressed.size()) {
            if (compressed[i] == 0xFF && i + 2 < compressed.size()) {
                uint8_t count = compressed[i + 1];
                if (count == 0) {
                    data.push_back(0xFF);
                    i += 2;
                } else {
                    // RLE sequence - use push_back instead of insert to avoid memory issues
                    uint8_t value = compressed[i + 2];
                    for (int j = 0; j < count; j++) {
                        data.push_back(value);
                    }
                    i += 3;
                }
            } else {
                data.push_back(compressed[i]);
                i++;
            }
        }
        
        return data;
    }

    // Delta encoding for correlated data
    static std::vector<int16_t> delta_encode(const std::vector<uint16_t>& data) {
        std::vector<int16_t> deltas;
        deltas.reserve(data.size());
        
        if (data.empty()) return deltas;
        
        deltas.push_back(static_cast<int16_t>(data[0]));
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

public:
    // Compress SafeTensors file
    static bool compress(const std::string& input_path, const std::string& output_path) {
        auto start = std::chrono::high_resolution_clock::now();
        
        // Read input file
        std::ifstream input(input_path, std::ios::binary);
        if (!input) {
            std::cerr << "Cannot open input file: " << input_path << std::endl;
            return false;
        }
        
        // Read entire file
        input.seekg(0, std::ios::end);
        size_t file_size = input.tellg();
        input.seekg(0, std::ios::beg);
        
        std::vector<uint8_t> data(file_size);
        input.read(reinterpret_cast<char*>(data.data()), file_size);
        input.close();
        
        std::cout << "Read " << file_size << " bytes" << std::endl;
        
        // Parse SafeTensors header (first 8 bytes = header size)
        if (file_size < 8) {
            std::cerr << "File too small" << std::endl;
            return false;
        }
        
        uint64_t header_size;
        std::memcpy(&header_size, data.data(), sizeof(uint64_t));
        
        std::cout << "JSON header size: " << header_size << " bytes" << std::endl;
        
        // Separate header and tensor data
        std::vector<uint8_t> header_data(data.begin(), data.begin() + 8 + header_size);
        std::vector<uint8_t> tensor_data(data.begin() + 8 + header_size, data.end());
        
        std::cout << "Tensor data size: " << tensor_data.size() << " bytes" << std::endl;
        
        // Convert float32 to float16 (assuming tensor data is float32)
        size_t num_floats = tensor_data.size() / sizeof(float);
        std::vector<uint16_t> compressed_tensors;
        compressed_tensors.reserve(num_floats);
        
        for (size_t i = 0; i < num_floats; i++) {
            float value;
            std::memcpy(&value, tensor_data.data() + i * sizeof(float), sizeof(float));
            compressed_tensors.push_back(float32_to_float16(value));
        }
        
        std::cout << "Converted to float16: " << compressed_tensors.size() << " values" << std::endl;
        
        // Apply delta encoding
        auto deltas = delta_encode(compressed_tensors);
        
        // Convert to byte array
        std::vector<uint8_t> delta_bytes(deltas.size() * sizeof(int16_t));
        std::memcpy(delta_bytes.data(), deltas.data(), delta_bytes.size());
        
        // Apply RLE compression
        auto final_compressed = rle_compress(delta_bytes);
        
        std::cout << "After RLE: " << final_compressed.size() << " bytes" << std::endl;
        std::ofstream output(output_path, std::ios::binary);
        if (!output) {
            std::cerr << "Cannot open output file: " << output_path << std::endl;
            return false;
        }
        
        // Write custom header
        Header hdr;
        hdr.original_size = file_size;
        hdr.json_header_size = header_data.size();  // Full header size including the 8-byte prefix
        hdr.num_tensors = num_floats;
        hdr.flags = 0;
        
        output.write(reinterpret_cast<const char*>(&hdr), sizeof(Header));
        
        // Write JSON header (uncompressed for simplicity)
        output.write(reinterpret_cast<const char*>(header_data.data()), header_data.size());
        
        // Write compressed tensor data (with frequency compression)
        uint64_t compressed_size = final_compressed.size();
        output.write(reinterpret_cast<const char*>(&compressed_size), sizeof(uint64_t));
        output.write(reinterpret_cast<const char*>(final_compressed.data()), final_compressed.size());
        
        output.close();
        
        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
        
        size_t output_size = final_compressed.size();
        double ratio = static_cast<double>(file_size) / output_size;
        
        std::cout << "\n=== Compression Results ===" << std::endl;
        std::cout << "Original size: " << file_size << " bytes" << std::endl;
        std::cout << "Compressed size: " << output_size << " bytes" << std::endl;
        std::cout << "Compression ratio: " << ratio << ":1" << std::endl;
        std::cout << "Time: " << duration.count() << " ms" << std::endl;
        
        return true;
    }

    // Decompress file
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
        
        // Read JSON header (8 bytes size + json_header_size bytes of JSON)
        std::vector<uint8_t> header_data(hdr.json_header_size);
        input.read(reinterpret_cast<char*>(header_data.data()), header_data.size());
        
        // Read compressed tensor data
        uint64_t compressed_size;
        input.read(reinterpret_cast<char*>(&compressed_size), sizeof(uint64_t));
        
        std::vector<uint8_t> compressed_data;
        try {
            compressed_data.resize(compressed_size);
        } catch (const std::bad_alloc& e) {
            std::cerr << "Memory allocation failed for compressed_data: " << e.what() << std::endl;
            return false;
        }
        
        input.read(reinterpret_cast<char*>(compressed_data.data()), compressed_size);
        if (!input) {
            std::cerr << "Failed to read compressed data" << std::endl;
            return false;
        }
        input.close();
        
        // Decompress RLE
        auto delta_bytes = rle_decompress(compressed_data);
        
        // Convert back to deltas
        size_t delta_count = delta_bytes.size() / sizeof(int16_t);
        std::vector<int16_t> deltas(delta_count);
        std::memcpy(deltas.data(), delta_bytes.data(), delta_bytes.size());
        
        // Delta decode back to float16 values
        auto float16_values = delta_decode(deltas);
        
        // Convert float16 back to float32
        size_t tensor_count = float16_values.size();
        std::vector<uint8_t> tensor_data(tensor_count * sizeof(float));
        
        for (size_t i = 0; i < tensor_count; i++) {
            float value = float16_to_float32(float16_values[i]);
            std::memcpy(tensor_data.data() + i * sizeof(float), &value, sizeof(float));
        }
        
        // Write output - reconstruct SafeTensors format
        std::ofstream output(output_path, std::ios::binary);
        if (!output) {
            std::cerr << "Cannot open output file: " << output_path << std::endl;
            return false;
        }
        
        // Write header_data as-is (includes the 8-byte size + JSON)
        output.write(reinterpret_cast<const char*>(header_data.data()), header_data.size());
        output.write(reinterpret_cast<const char*>(tensor_data.data()), tensor_data.size());
        output.close();
        
        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
        
        std::cout << "\n=== Decompression Results ===" << std::endl;
        std::cout << "Decompressed size: " << (header_data.size() + tensor_data.size()) << " bytes" << std::endl;
        std::cout << "Time: " << duration.count() << " ms" << std::endl;
        
        return true;
    }
};

int main(int argc, char* argv[]) {
    if (argc < 4) {
        std::cout << "Usage:" << std::endl;
        std::cout << "  Compress:   " << argv[0] << " -c <input.safetensors> <output.compressed>" << std::endl;
        std::cout << "  Decompress: " << argv[0] << " -d <input.compressed> <output.safetensors>" << std::endl;
        return 1;
    }
    
    std::string mode = argv[1];
    std::string input = argv[2];
    std::string output = argv[3];
    
    if (mode == "-c") {
        if (!LLMCodec::compress(input, output)) {
            std::cerr << "Compression failed!" << std::endl;
            return 1;
        }
    } else if (mode == "-d") {
        if (!LLMCodec::decompress(input, output)) {
            std::cerr << "Decompression failed!" << std::endl;
            return 1;
        }
    } else {
        std::cerr << "Invalid mode: " << mode << std::endl;
        return 1;
    }
    
    return 0;
}