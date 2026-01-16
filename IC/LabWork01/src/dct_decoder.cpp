#include <iostream>
#include <fstream>
#include <vector>
#include <cmath>
#include <sndfile.hh>
#include "bit_stream.h"
#include <cstring>

using namespace std;

vector<short> idct(const vector<double>& dct_coeffs, int block_size) {
    vector<short> result(block_size);
    const double factor = M_PI / static_cast<double>(block_size);
    
    for (int n = 0; n < block_size; n++) {
        double sum = 0.0;
        for (int k = 0; k < dct_coeffs.size(); k++) {
            double coeff = dct_coeffs[k];
            if (k == 0) {
                coeff *= 1.0 / sqrt(2.0);
            }
            sum += coeff * cos(factor * (n + 0.5) * k);
        }
        double sample = sum * sqrt(2.0 / block_size);
        
        if (sample > 32767.0) sample = 32767.0;
        if (sample < -32768.0) sample = -32768.0;
        
        result[n] = static_cast<short>(round(sample));
    }
    
    return result;
}

int main(int argc, char *argv[]) {
    if (argc < 3) {
        cerr << "Usage: " << argv[0] << " <input_encoded_file> <output_wav_file>\n";
        return 1;
    }

    fstream ifs { argv[1], ios::in | ios::binary };
    if (!ifs.is_open()) {
        cerr << "Error: cannot open input file\n";
        return 1;
    }
    BitStream ibs { ifs, STREAM_READ };

    uint64_t num_frames = ibs.read_n_bits(32);
    uint64_t sample_rate = ibs.read_n_bits(32);
    uint64_t block_size = ibs.read_n_bits(16);
    uint64_t discarded_samples = ibs.read_n_bits(16);
    uint64_t quant_high = ibs.read_n_bits(32);
    uint64_t quant_low = ibs.read_n_bits(32);
    uint64_t quant_bits = (quant_high << 32) | quant_low;
    double quant_step;
    memcpy(&quant_step, &quant_bits, sizeof(quant_step));

    cout << "Decoding DCT file with:\n";
    cout << "  Frames: " << num_frames << "\n";
    cout << "  Sample rate: " << sample_rate << "\n";
    cout << "  Block size: " << block_size << "\n";
    cout << "  Discarded samples: " << discarded_samples << "\n";
    cout << "  Quantization step: " << quant_step << "\n";

    if (discarded_samples >= block_size) {
        cerr << "Error: invalid discarded_samples value\n";
        ibs.close();
        return 1;
    }

    int format = SF_FORMAT_WAV | SF_FORMAT_PCM_16;
    SndfileHandle sfhOut { argv[2], SFM_WRITE, format, 1, static_cast<int>(sample_rate) };
    
    if (sfhOut.error()) {
        cerr << "Error: cannot create output file\n";
        ibs.close();
        return 1;
    }

    size_t total_blocks = (num_frames + block_size - 1) / block_size;
    size_t kept_coeffs = block_size - discarded_samples;
    vector<short> all_samples;
    for (size_t b = 0; b < total_blocks; b++) {
        vector<double> dct_coeffs(block_size, 0.0);
        for (size_t i = 0; i < kept_coeffs; i++) {
            int sign_bit = ibs.read_bit();
            uint16_t abs_val = ibs.read_n_bits(16);
            int32_t val = static_cast<int32_t>(abs_val);
            if (sign_bit) val = -val;
            dct_coeffs[i] = static_cast<double>(val) * quant_step;
        }

        vector<short> block_samples = idct(dct_coeffs, block_size);
        all_samples.insert(all_samples.end(), block_samples.begin(), block_samples.end());
    }

    all_samples.resize(num_frames);
    sfhOut.writef(all_samples.data(), num_frames);
    ibs.close();

    cout << "Decoding completed successfully.\n";
    cout << "Total frames decoded: " << num_frames << "\n";
    cout << "Output file: " << argv[2] << "\n";

    return 0;
}