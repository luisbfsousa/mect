#ifndef IMAGE_CODEC_H
#define IMAGE_CODEC_H

#include <vector>
#include <string>
#include "golomb.h"

class ImageCodec {
public:
    enum class Predictor {
        PREV_PIXEL,         // P(x, y) = pixel to the left
        ABOVE_PIXEL,        // P(x, y) = pixel above
        AVERAGE_PREDICTOR,  // P(x, y) = average of left and above pixels
        PAETH_PREDICTOR,    // P(x, y) = Paeth predictor
        JPEG_LS_PREDICTOR,  // P(x, y) = left + above - upper left
        GRADIENT_PREDICTOR  // P(x, y) = left + (above - upper left)/2
    };

private:
    Predictor predictor;
    unsigned int optimalM;
    int predictPixel(const std::vector<unsigned char>& image, int width, int x, int y) const;
    unsigned int calculateOptimalM(const std::vector<int>& residuals) const;
    int paethPredictor(int a, int b, int c) const;

public:
    ImageCodec(Predictor pred = Predictor::AVERAGE_PREDICTOR);
    std::vector<bool> encode(const std::vector<unsigned char>& image, int width, int height);
    std::vector<unsigned char> decode(const std::vector<bool>& encoded, int width, int height);
    void setPredictor(Predictor pred) { predictor = pred; }
    Predictor getPredictor() const { return predictor; }
};

#endif