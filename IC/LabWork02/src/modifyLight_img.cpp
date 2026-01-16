#include <opencv2/opencv.hpp>
#include <iostream>

using namespace std;
using namespace cv;

int main(int argc, char **argv) {
    if (argc != 4) {
        cerr << "Usage: " << argv[0] << " <input_image> <output_image> <brightness_value>" << endl;
        return -1;
    }

    Mat inputImage = imread(argv[1]);
    if (inputImage.empty()) {
        cerr << "Error: Could not open or find the image!" << endl;
        return -1;
    }

    Mat brightenedImage = Mat::zeros(inputImage.size(), inputImage.type());
    int brightness = stoi(argv[3]);

    if (brightness < -255 || brightness > 255) {
        cerr << "Error: Brightness value must be in the range [-255, 255]!" << endl;
        return -1;
    }

    for (int i = 0; i < inputImage.rows; i++) {
        for (int j = 0; j < inputImage.cols; j++) {
            for (int c = 0; c < inputImage.channels(); c++) {
                int newValue = inputImage.at<Vec3b>(i, j)[c] + brightness;
                brightenedImage.at<Vec3b>(i, j)[c] = saturate_cast<uchar>(newValue);
            }
        }
    }

    if (!imwrite(argv[2], brightenedImage)) {
        cerr << "Error: Could not save the brightened image!" << endl;
        return -1;
    }

    return 0;
}