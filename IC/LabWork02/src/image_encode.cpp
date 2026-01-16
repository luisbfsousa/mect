#include <opencv2/opencv.hpp>
#include <iostream>
#include <fstream>
#include "image_codec.h"

void saveEncodedImage(const std::vector<bool>& encoded, const std::string& filename) {
    std::ofstream outFile(filename, std::ios::binary);
    unsigned char byte = 0;
    int bitCount = 0;
    
    for (bool bit : encoded) {
        byte = (byte << 1) | (bit ? 1 : 0);
        bitCount++;
        
        if (bitCount == 8) {
            outFile.put(byte);
            byte = 0;
            bitCount = 0;
        }
    }
    
    if (bitCount > 0) {
        byte <<= (8 - bitCount);
        outFile.put(byte);
    }
    
    outFile.close();
}

int main(int argc, char** argv) {
    if (argc != 3) {
        std::cerr << "Usage: " << argv[0] << " <input_image> <output_file>\n";
        return 1;
    }
    
    std::string inputPath = argv[1];
    std::string outputPath = argv[2];
    ImageCodec codec(ImageCodec::Predictor::PAETH_PREDICTOR);
    cv::Mat image = cv::imread(inputPath, cv::IMREAD_GRAYSCALE);
    if (image.empty()) {
        std::cerr << "Error: Could not read image " << inputPath << std::endl;
        return 1;
    }
    
    std::vector<unsigned char> imageData(image.data, image.data + image.total());
    auto encoded = codec.encode(imageData, image.cols, image.rows);
    saveEncodedImage(encoded, outputPath);
    
    double originalSize = image.total();
    double compressedSize = encoded.size() / 8.0;
    std::cout << "Original size: " << originalSize << " bytes\n";
    std::cout << "Compressed size: " << compressedSize << " bytes\n";
    std::cout << "Compression ratio: " << originalSize / compressedSize << ":1\n";
    
    return 0;
}