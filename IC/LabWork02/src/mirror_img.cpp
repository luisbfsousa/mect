#include <opencv2/opencv.hpp>
#include <iostream>

using namespace std;
using namespace cv;

int main(int argc, char **argv) {
    if (argc != 4) {
        cerr << "Usage: " << argv[0] << " <input_image> <output_image> <mirror_type>" << endl;
        return -1;
    }

    Mat inputImage = imread(argv[1]);
    if (inputImage.empty()) {
        cerr << "Error: Could not open or find the image!" << endl;
        return -1;
    }

    string mirrorType = argv[3];
    Mat mirroredImage = Mat::zeros(inputImage.size(), inputImage.type());

    if (mirrorType == "h") {
        for (int i = 0; i < inputImage.rows; i++) {
            for (int j = 0; j < inputImage.cols; j++) {
                mirroredImage.at<Vec3b>(i, j) = inputImage.at<Vec3b>(i, inputImage.cols - j - 1);
            }
        }

        if (!imwrite(argv[2], mirroredImage)) {
            cerr << "Error: Could not save the mirrored image!" << endl;
            return -1;
        }
    } else if (mirrorType == "v") {
        for (int i = 0; i < inputImage.rows; i++) {
            for (int j = 0; j < inputImage.cols; j++) {
                mirroredImage.at<Vec3b>(i, j) = inputImage.at<Vec3b>(inputImage.rows - i - 1, j);
            }
        }
        
        if (!imwrite(argv[2], mirroredImage)) {
            cerr << "Error: Could not save the mirrored image!" << endl;
            return -1;
        }
    } else {
        cerr << "Error: Invalid mirror type! Use 'h' for horizontal or 'v' for vertical." << endl;
        return -1;
    }
    
    return 0;
}