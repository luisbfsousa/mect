#include <iostream>
#include <vector>
#include <sndfile.hh>

using namespace std;

constexpr size_t FRAMES_BUFFER_SIZE = 65536;

int main(int argc, char *argv[]) {
    if(argc < 4) {
        cerr << "Usage: " << argv[0] << " <input.wav> <bits_to_keep> <output.wav>\n";
        return 1;
    }

    // Open input file
    SndfileHandle sfhIn { argv[1] };
    if(sfhIn.error()) {
        cerr << "Error: invalid input file\n";
        return 1;
    }

    // Check WAV format and PCM_16
    if((sfhIn.format() & SF_FORMAT_TYPEMASK) != SF_FORMAT_WAV || 
       (sfhIn.format() & SF_FORMAT_SUBMASK) != SF_FORMAT_PCM_16) {
        cerr << "Error: file must be WAV PCM_16 format\n";
        return 1;
    }

    // Get bits to keep
    int bits_to_keep = stoi(argv[2]);
    if (bits_to_keep < 1 || bits_to_keep > 16) {
        cerr << "Error: bits_to_keep must be between 1 and 16\n";
        return 1;
    }

    // Open output file
    SndfileHandle sfhOut { argv[3], SFM_WRITE, sfhIn.format(),
        sfhIn.channels(), sfhIn.samplerate() };
    if(sfhOut.error()) {
        cerr << "Error: cannot create output file\n";
        return 1;
    }

    int bits_to_remove = 16 - bits_to_keep;
    vector<short> samples(FRAMES_BUFFER_SIZE * sfhIn.channels());

    // Process audio in chunks
    size_t nFrames;
    while((nFrames = sfhIn.readf(samples.data(), FRAMES_BUFFER_SIZE))) {
        // Quantize each sample in the current chunk
        for(size_t i = 0; i < nFrames * sfhIn.channels(); i++) {
            samples[i] = (samples[i] >> bits_to_remove) << bits_to_remove;
        }
        
        // Write quantized chunk to output
        sfhOut.writef(samples.data(), nFrames);
    }

    cout << "Quantized to " << bits_to_keep << " bits per sample\n";
    return 0;
}