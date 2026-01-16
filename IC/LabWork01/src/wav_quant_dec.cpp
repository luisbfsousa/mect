#include "bit_stream.h"
#include <iostream>
#include <vector>
#include <sndfile.hh>
#include <fstream>

using namespace std;

constexpr size_t FRAMES_BUFFER_SIZE = 65536;

int main(int argc, char *argv[]) {
    if(argc != 3) {
        cerr << "Usage: " << argv[0] << " <input.encoded> <output.wav>\n";
        return 1;
    }

    fstream ifs { argv[1], ios::in | ios::binary };
    if(!ifs.is_open()) {
        cerr << "Error: cannot open input file\n";
        return 1;
    }
    BitStream ibs { ifs, STREAM_READ };

    uint64_t num_frames = ibs.read_n_bits(32);
    uint64_t num_channels = ibs.read_n_bits(32);
    uint64_t sample_rate = ibs.read_n_bits(32);
    uint64_t bits_to_keep = ibs.read_n_bits(8);

    cout << "Decoding file with:\n";
    cout << "  Frames: " << num_frames << "\n";
    cout << "  Channels: " << num_channels << "\n";
    cout << "  Sample rate: " << sample_rate << "\n";
    cout << "  Bits per sample: " << bits_to_keep << "\n";

    if(bits_to_keep < 1 || bits_to_keep > 16) {
        cerr << "Error: invalid bits_to_keep value: " << bits_to_keep << "\n";
        ibs.close();
        return 1;
    }

    int format = SF_FORMAT_WAV | SF_FORMAT_PCM_16;
    SndfileHandle sfhOut { argv[2], SFM_WRITE, format, 
        static_cast<int>(num_channels), static_cast<int>(sample_rate) };
    
    if(sfhOut.error()) {
        cerr << "Error: cannot create output file\n";
        ibs.close();
        return 1;
    }

    int bits_to_remove = 16 - bits_to_keep;
    vector<short> samples(FRAMES_BUFFER_SIZE * num_channels);

    size_t total_samples = num_frames * num_channels;
    size_t samples_read = 0;

    while(samples_read < total_samples) {
        size_t samples_to_read = min(
            static_cast<size_t>(FRAMES_BUFFER_SIZE * num_channels), 
            total_samples - samples_read
        );
        
        for(size_t i = 0; i < samples_to_read; i++) {
            uint16_t quantized_value = ibs.read_n_bits(bits_to_keep);
            
            uint16_t unsigned_sample = quantized_value << bits_to_remove;
            
            samples[i] = static_cast<short>(unsigned_sample - 32768);
        }
        
        size_t frames_to_write = samples_to_read / num_channels;
        sfhOut.writef(samples.data(), frames_to_write);
        
        samples_read += samples_to_read;
    }

    ibs.close();

    cout << "Decoding completed successfully.\n";
    cout << "Total samples decoded: " << total_samples << "\n";
    cout << "Output file: " << argv[2] << "\n";

    return 0;
}