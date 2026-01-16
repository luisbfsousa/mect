#include <opencv2/opencv.hpp>
#include <iostream>

using namespace std;
using namespace cv;

int main(int argc, char **argv) {
    if (argc != 4) {
        cerr << "Usage: " << argv[0] << " <input_image> <output_image> <rotation_angle>" << endl;
        return -1;
    }

    Mat inputImage = imread(argv[1]);
    if (inputImage.empty()) {
        cerr << "Error: Could not open or find the image!" << endl;
        return -1;
    }

    int angle = stoi(argv[3]);
    if (angle % 90 != 0) {
        cerr << "Error: Rotation angle must be a multiple of 90!" << endl;
        return -1;
    }

    Mat rotatedImage = Mat::zeros(inputImage.size(), inputImage.type());
    int numRotations = ((angle / 90) + 4) % 4;
    if (numRotations <= 0) {
        numRotations += 4;
    }
    for (int r = 0; r < numRotations; r++) {
        Mat tempImage = Mat::zeros(rotatedImage.size(), rotatedImage.type());
        for (int i = 0; i < inputImage.rows; i++) {
            for (int j = 0; j < inputImage.cols; j++) {
                tempImage.at<Vec3b>(j, inputImage.rows - i - 1) = inputImage.at<Vec3b>(i, j);
            }
        }
        rotatedImage = tempImage;
        inputImage = rotatedImage;
    }

    if (!imwrite(argv[2], rotatedImage)) {
        cerr << "Error: Could not save the rotated image!" << endl;
        return -1;
    }
    
    return 0;
}