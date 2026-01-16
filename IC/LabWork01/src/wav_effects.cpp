#include <iostream>
#include <vector>
#include <sndfile.hh>
#include <cmath>
#include <string>
#include <algorithm>
#include "wav_hist.h"

using namespace std;

constexpr size_t FRAMES_BUFFER_SIZE = 65536; // Buffer for reading frames

// Audio effect functions
void applyEcho(vector<short>& samples, float delay, float gain, int sampleRate, int channels) {
    // Echo: y(n) = x(n) + gain * x(n - delay)
    vector<short> original = samples;
    int delay_samples = static_cast<int>(delay * sampleRate) * channels;

    for (size_t i = delay_samples; i < samples.size(); i++) {
        float value = samples[i] + gain * original[i - delay_samples];
        samples[i] = static_cast<short>(std::clamp(value, -32768.0f, 32767.0f));
    }
}

void applyMultipleEchoes(vector<short>& samples, float initial_delay, int num_echoes, float gain, int sampleRate, int channels) {
    // Multiple Echoes: y(n) = x(n) + gain^k * x(n - k*delay)
    vector<short> original = samples;
    int delay_samples = static_cast<int>(initial_delay * sampleRate) * channels;
    
    for (int echo = 1; echo <= num_echoes; echo++) {
        float current_gain = pow(gain, echo);
        int current_delay = delay_samples * echo;
        
        for (size_t i = current_delay; i < samples.size(); i++) {
            float value = samples[i] + current_gain * original[i - current_delay];
            samples[i] = static_cast<short>(std::clamp(value, -32768.0f, 32767.0f));
        }
    }
}

void applyAmplitudeModulation(vector<short>& samples, float frequency, float depth, int sampleRate, int channels) {
    // Amplitude Modulation: y(n) = x(n) * (1 + depth * sin(2 * pi * f * t))
    for (size_t i = 0; i < samples.size(); i++) {
        float time = static_cast<float>((i / channels)) / sampleRate;
        float modulation = 1.0f + depth * sin(2.0f * M_PI * frequency * time);
        float value = samples[i] * modulation;
        samples[i] = static_cast<short>(std::clamp(value, -32768.0f, 32767.0f));
    }
}

void applyTimeVaryingDelay(vector<short>& samples, float max_delay, float lfo_freq, int sampleRate, int channels) {
    // Time-Varying Delay: y(n) = x(n) + 0.3 * x(n - d(n)) [where d(n) varies with LFO]
    vector<short> original = samples;
    int max_delay_samples = static_cast<int>(max_delay * sampleRate) * channels;
    
    for (size_t i = 0; i < samples.size(); i++) {
        float time = static_cast<float>((i / channels)) / sampleRate;
        float current_delay = (max_delay_samples / 2.0f) * 
                            (1.0f + sin(2.0f * M_PI * lfo_freq * time));
        int delay_idx = static_cast<int>(current_delay);
        
        if (i >= delay_idx) {
            float value = 0.7f * samples[i] + 0.3f * original[i - delay_idx];
            samples[i] = static_cast<short>(std::clamp(value, -32768.0f, 32767.0f));
        }
    }
}

int main(int argc, char *argv[]) {
    if (argc < 5) {
        cerr << "Usage: wav_effects <input_file> <output_file> <effect> [parameters] \n"
             << "Effects:\n"
             << "  echo <delay> <gain>\n"
             << "  multi_echo <initial_delay> <num_echoes> <gain>\n"
             << "  amplitude_mod <frequency> <depth>\n"
             << "  varying_delay <max_delay> <lfo_freq>\n";
        return 1;
    }

    SndfileHandle sfhIn { argv[1] };
    if (sfhIn.error()) {
        cerr << "Error: invalid input file\n";
        return 1;
    }

    if ((sfhIn.format() & SF_FORMAT_TYPEMASK) != SF_FORMAT_WAV ||
        (sfhIn.format() & SF_FORMAT_SUBMASK) != SF_FORMAT_PCM_16) {
        cerr << "Error: file must be in WAV format (PCM 16 bits)\n";
        return 1;
    }

    SndfileHandle sfhOut { argv[2], SFM_WRITE, sfhIn.format(),
        sfhIn.channels(), sfhIn.samplerate() };
    if (sfhOut.error()) {
        cerr << "Error: invalid output file\n";
        return 1;
    }

    string effect = argv[3];
    vector<short> samples(FRAMES_BUFFER_SIZE * sfhIn.channels());
    vector<short> processed_samples;

    size_t nFrames;
    while ((nFrames = sfhIn.readf(samples.data(), FRAMES_BUFFER_SIZE))) {
        samples.resize(nFrames * sfhIn.channels());
        processed_samples.insert(processed_samples.end(), samples.begin(), samples.end());
    }

    if (effect == "echo" && argc == 6) {
        float delay = stof(argv[4]);
        float gain = stof(argv[5]);
        applyEcho(processed_samples, delay, gain, sfhIn.samplerate(), sfhIn.channels());
    }
    else if (effect == "multi_echo" && argc == 7) {
        float initial_delay = stof(argv[4]);
        int num_echoes = stoi(argv[5]);
        float gain = stof(argv[6]);
        applyMultipleEchoes(processed_samples, initial_delay, num_echoes, 
                           gain, sfhIn.samplerate(), sfhIn.channels());
    }
    else if (effect == "amplitude_mod" && argc == 6) {
        float frequency = stof(argv[4]);
        float depth = stof(argv[5]);
        applyAmplitudeModulation(processed_samples, frequency, depth, 
                                sfhIn.samplerate(), sfhIn.channels());
    }
    else if (effect == "varying_delay" && argc == 6) {
        float max_delay = stof(argv[4]);
        float lfo_freq = stof(argv[5]);
        applyTimeVaryingDelay(processed_samples, max_delay, lfo_freq, 
                             sfhIn.samplerate(), sfhIn.channels());
    }
    else {
        cerr << "Error: invalid effect or parameters\n"
             << "Available effects: echo, multi_echo, amplitude_mod, varying_delay\n";
        return 1;
    }

    sfhOut.writef(processed_samples.data(),
                   processed_samples.size() / sfhIn.channels());

    return 0;
}