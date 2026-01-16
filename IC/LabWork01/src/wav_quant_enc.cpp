#include "bit_stream.h"
#include <iostream>
#include <vector>
#include <sndfile.hh>
#include <fstream>
#include <iomanip>

using namespace std;

constexpr size_t FRAMES_BUFFER_SIZE = 65536;

int main(int argc, char *argv[]) {
    if(argc != 4) {
        cerr << "Usage: " << argv[0] << " <input.wav> <output.encoded> <bits_to_keep>\n";
        cerr << "bits_to_keep: number of bits to keep (1-16)\n";
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

    int bits_to_keep = stoi(argv[3]);
    if (bits_to_keep < 1 || bits_to_keep > 16) {
        cerr << "Error: bits_to_keep must be between 1 and 16\n";
        return 1;
    }

    fstream ofs { argv[2], ios::out | ios::binary };
    if(not ofs.is_open()) {
        cerr << "Error: cannot create output file\n";
        return 1;
    }
    BitStream obs { ofs, STREAM_WRITE };
    
    obs.write_n_bits(sfhIn.frames(), 32);     // Number of frames
    obs.write_n_bits(sfhIn.channels(), 32);   // Number of channels
    obs.write_n_bits(sfhIn.samplerate(), 32); // Sample rate
    obs.write_n_bits(bits_to_keep, 8);        // Bits per sample

    int bits_to_remove = 16 - bits_to_keep;
    vector<short> samples(FRAMES_BUFFER_SIZE * sfhIn.channels());

    size_t nFrames;
    while((nFrames = sfhIn.readf(samples.data(), FRAMES_BUFFER_SIZE))) {
        size_t total_samples = nFrames * sfhIn.channels();

        for(size_t i = 0; i < total_samples; i++) {

            uint16_t unsigned_quantized = samples[i] + 32768;
            uint16_t bits_to_write = (unsigned_quantized) >> bits_to_remove;
            obs.write_n_bits(bits_to_write, bits_to_keep);
        }
    }

    obs.close();

    cout << "Encoding completed successfully.\n";
    cout << "Original bit depth: 16 bits\n";
    cout << "Quantized to: " << bits_to_keep << " bits\n";
    cout << "Compression ratio: " << fixed << setprecision(2) 
         << (16.0 / bits_to_keep) << ":1\n";

    return 0;
}