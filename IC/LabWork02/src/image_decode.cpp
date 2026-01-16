#include <opencv2/opencv.hpp>
#include <iostream>
#include <fstream>
#include "image_codec.h"

std::vector<bool> loadEncodedImage(const std::string& filename) {
    std::ifstream inFile(filename, std::ios::binary);
    std::vector<bool> encoded;
    
    char byte;
    while (inFile.get(byte)) {
        for (int i = 7; i >= 0; i--) {
            encoded.push_back((byte >> i) & 1);
        }
    }
    
    inFile.close();
    return encoded;
}

int main(int argc, char** argv) {
    if (argc != 3) {
        std::cerr << "Usage: " << argv[0] << " <input_file> <output_image>\n";
        return 1;
    }
    
    std::string inputPath = argv[1];
    std::string outputPath = argv[2];
    ImageCodec codec(ImageCodec::Predictor::PAETH_PREDICTOR);
    auto encoded = loadEncodedImage(inputPath);
    int width = 0, height = 0;
    for (int i = 0; i < 16; i++) width = (width << 1) | encoded[i];
    for (int i = 16; i < 32; i++) height = (height << 1) | encoded[i];
    auto decoded = codec.decode(encoded, width, height);
    cv::Mat grayImage(height, width, CV_8UC1, decoded.data());
    cv::Mat bgrImage;
    cv::cvtColor(grayImage, bgrImage, cv::COLOR_GRAY2BGR);
    if (!cv::imwrite(outputPath, bgrImage)) {
        std::cerr << "Error: Could not save image to " << outputPath << std::endl;
        return 1;
    }
    
    std::cout << "Image successfully decoded and saved to " << outputPath << std::endl;
    
    return 0;
}