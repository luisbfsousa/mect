#include <opencv2/opencv.hpp>
#include <iostream>

using namespace std;
using namespace cv;

int main(int argc, char** argv) {
    if (argc != 3) {
        cerr << "Usage: " << argv[0] << " <input_image> <output_image>" << endl;
        return -1;
    }

    Mat inputImage = imread(argv[1]);
    if (inputImage.empty()) {
        cerr << "Error: Could not open or find the image!" << endl;
        return -1;
    }

    Mat negativeImage = Mat::zeros(inputImage.size(), inputImage.type());
    
    for (int i = 0; i < inputImage.rows; i++) {
        for (int j = 0; j < inputImage.cols; j++) {
            Vec3b pixel = inputImage.at<Vec3b>(i, j);
            
            negativeImage.at<Vec3b>(i, j)[0] = 255 - pixel[0];  // Blue channel
            negativeImage.at<Vec3b>(i, j)[1] = 255 - pixel[1];  // Green channel
            negativeImage.at<Vec3b>(i, j)[2] = 255 - pixel[2];  // Red channel
        }
    }

    if (!imwrite(argv[2], negativeImage)) {
        cerr << "Error: Could not save the negative image!" << endl;
        return -1;
    }

    return 0;
}