#include <iostream>
#include <vector>
#include <sndfile.hh>

using namespace std;

constexpr size_t FRAMES_BUFFER_SIZE = 65536;

int main(int argc, char *argv[]) {
    if(argc < 3) {
        cerr << "Usage: " << argv[0] << " <input.wav> <output.wav>\n";
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

    if(sfhIn.channels() == 1) {
        cerr << "File is already mono, copying...\n";
        SndfileHandle sfhOut { argv[2], SFM_WRITE, sfhIn.format(),
            sfhIn.channels(), sfhIn.samplerate() };
        
        vector<short> samples(FRAMES_BUFFER_SIZE);
        size_t nFrames;
        while((nFrames = sfhIn.readf(samples.data(), FRAMES_BUFFER_SIZE))) {
            sfhOut.writef(samples.data(), nFrames);
        }
        return 0;
    }

    int format = SF_FORMAT_WAV | SF_FORMAT_PCM_16;
    SndfileHandle sfhOut { argv[2], SFM_WRITE, format, 1, sfhIn.samplerate() };
    
    if(sfhOut.error()) {
        cerr << "Error: cannot create output file\n";
        return 1;
    }

    vector<short> samples(FRAMES_BUFFER_SIZE * sfhIn.channels());
    vector<short> mono_samples(FRAMES_BUFFER_SIZE);

    size_t nFrames;
    while((nFrames = sfhIn.readf(samples.data(), FRAMES_BUFFER_SIZE))) {
        for(size_t i = 0; i < nFrames; i++) {
            int sum = 0;
            for(int ch = 0; ch < sfhIn.channels(); ch++) {
                sum += samples[i * sfhIn.channels() + ch];
            }
            mono_samples[i] = static_cast<short>(sum / sfhIn.channels());
        }
        sfhOut.writef(mono_samples.data(), nFrames);
    }

    cout << "Converted to mono\n";
    return 0;
}