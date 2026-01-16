#include <opencv2/opencv.hpp>
#include <iostream>

using namespace std;
using namespace cv;

int main(int argc, char** argv) {
    if (argc != 4) {
        cerr << "Usage: " << argv[0] << " <input_image> <output_image> <channel_number>\n";
        cerr << "Channel number: 0 = Blue, 1 = Green, 2 = Red\n";
        return 1;
    }

    string inputFile = argv[1];
    string outputFile = argv[2];
    int channel = stoi(argv[3]);

    if (channel < 0 || channel > 2) {
        cerr << "Error: channel number must be 0, 1, or 2\n";
        return 1;
    }

    Mat image = imread(inputFile, IMREAD_COLOR);
    if (image.empty()) {
        cerr << "Error: cannot open image file " << inputFile << endl;
        return 1;
    }

    Mat output(image.rows, image.cols, CV_8UC1);
    for (int i = 0; i < image.rows; ++i) {
        for (int j = 0; j < image.cols; ++j) {
            Vec3b pixel = image.at<Vec3b>(i, j);
            output.at<uchar>(i, j) = pixel[channel];
        }
    }

    // Convert single-channel to 3-channel for PPM format
    Mat outputBGR;
    cvtColor(output, outputBGR, COLOR_GRAY2BGR);

    if (!imwrite(outputFile, outputBGR)) {
        cerr << "Error: could not save output image to " << outputFile << endl;
        return 1;
    }

    cout << "Extracted channel " << channel << " from " << inputFile
         << " and saved result to " << outputFile << endl;

    return 0;
}
