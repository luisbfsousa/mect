#include <iostream>
#include <fstream>
#include <vector>
#include <cmath>
#include <sndfile.hh>
#include "bit_stream.h"
#include <cstring>

using namespace std;

constexpr size_t FRAMES_BUFFER_SIZE = 65536;

vector<double> dct(const vector<short>& block, int block_size) {
    vector<double> result(block_size);
    const double factor = M_PI / static_cast<double>(block_size);
    for (int k = 0; k < block_size; k++) {
        double sum = 0.0;
        for (int n = 0; n < block_size; n++) {
            sum += block[n] * cos(factor * (n + 0.5) * k);
        }
        result[k] = sum * sqrt(2.0 / block_size);
        if (k == 0) {
            result[k] *= 1.0 / sqrt(2.0);
        }
    }
    return result;
}

vector<int32_t> quantize(const vector<double>& dct_values, int discarded_samples, double quantization_step) {
    int block_size = dct_values.size();
    vector<int32_t> quantized(block_size - discarded_samples);
    for (int i = 0; i < block_size - discarded_samples; i++) {
        quantized[i] = static_cast<int32_t>(round(dct_values[i] / quantization_step));
    }
    return quantized;
}

int main(int argc, char *argv[]) {
    if (argc < 5) {
        cerr << "Usage: " << argv[0] << " <input_file> <output_file> <block_size> <discarded_samples> [quantization_step]\n";
        cerr << "  quantization_step: default 1.0 (smaller = better quality, larger file)\n";
        return 1;
    }

    SndfileHandle sfhIn { argv[1] };
    if(sfhIn.error()) {
        cerr << "Error: invalid input file\n";
        return 1;
    }

    if((sfhIn.format() & SF_FORMAT_TYPEMASK) != SF_FORMAT_WAV || 
       (sfhIn.format() & SF_FORMAT_SUBMASK) != SF_FORMAT_PCM_16) {
        cerr << "Error: file must be WAV PCM_16 format\n";
        return 1;
    }

    int block_size = stoi(argv[3]);
    int discarded_samples = stoi(argv[4]);
    double quant_step = (argc >= 6) ? stod(argv[5]) : 1.0;

    if(discarded_samples >= block_size) {
        cerr << "Error: discarded_samples must be less than block_size\n";
        return 1;
    }

    if(sfhIn.channels() != 1) {
        cerr << "Error: only mono files are supported\n";
        return 1;
    }

    cout << "Encoding with:\n";
    cout << "  Block size: " << block_size << "\n";
    cout << "  Discarded samples: " << discarded_samples << "\n";
    cout << "  Quantization step: " << quant_step << "\n";

    fstream ofs { argv[2], ios::out | ios::binary };
    if(not ofs.is_open()) {
        cerr << "Error: cannot create output file\n";
        return 1;
    }
    BitStream obs { ofs, STREAM_WRITE };

    obs.write_n_bits(sfhIn.frames(), 32);
    obs.write_n_bits(sfhIn.samplerate(), 32);
    obs.write_n_bits(block_size, 16);
    obs.write_n_bits(discarded_samples, 16);
    
    uint64_t quant_bits;
    memcpy(&quant_bits, &quant_step, sizeof(quant_step));
    obs.write_n_bits(quant_bits >> 32, 32);
    obs.write_n_bits(quant_bits & 0xFFFFFFFF, 32);

    vector<short> samples(block_size);
    size_t total_blocks = (sfhIn.frames() + block_size - 1) / block_size;
    size_t last_block = sfhIn.frames() % block_size;
    if (last_block == 0) 
        last_block = block_size;
    
    for (size_t b = 0; b < total_blocks; b++) {
        size_t current_block_size = (b == total_blocks - 1) ? last_block : block_size;
        sfhIn.readf(samples.data(), current_block_size);
        if (current_block_size < block_size) {
            fill(samples.begin() + current_block_size, samples.end(), 0);
        }
        vector<double> dct_values = dct(samples, block_size);
        vector<int32_t> quant = quantize(dct_values, discarded_samples, quant_step);
        for(int i = 0; i < block_size - discarded_samples; i++) {
            int32_t val = quant[i];
            obs.write_bit(val < 0 ? 1 : 0);
            uint16_t abs_val = static_cast<uint16_t>(abs(val));
            obs.write_n_bits(abs_val, 16);
        }
    }
    
    obs.close();
    cout << "Encoding completed successfully.\n";
    cout << "Output file: " << argv[2] << "\n";

    return 0;
}