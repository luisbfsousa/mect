#include <iostream>
#include <vector>
#include <sndfile.hh>
#include <cmath>

using namespace std;

constexpr size_t FRAMES_BUFFER_SIZE = 65536; // Buffer for reading frames

int main(int argc, char *argv[]) {

    if (argc < 3) {
        cerr << "Usage: " << argv[0] << "<original> <modified>\n";
        return 1;
    }

    SndfileHandle sndFileOg { argv[argc-2] };
	SndfileHandle sndFileMod { argv[argc-1] };

	if(sndFileOg.error()) {
		cerr << "Error: invalid original file\n";
		return 1;
    }
	if(sndFileMod.error()) {
		cerr << "Error: invalid modified file\n";
		return 1;
    }

    if((sndFileOg.format() & SF_FORMAT_TYPEMASK) != SF_FORMAT_WAV) {
		cerr << "Error: original file is not in WAV format\n";
		return 1;
	}
	if((sndFileOg.format() & SF_FORMAT_SUBMASK) != SF_FORMAT_PCM_16) {
		cerr << "Error: original file is not in PCM_16 format\n";
		return 1;
	}

	if((sndFileMod.format() & SF_FORMAT_TYPEMASK) != SF_FORMAT_WAV) {
		cerr << "Error: modified file is not in WAV format\n";
		return 1;
	}
	if((sndFileMod.format() & SF_FORMAT_SUBMASK) != SF_FORMAT_PCM_16) {
		cerr << "Error: modified file is not in PCM_16 format\n";
		return 1;
	}

	if (sndFileOg.channels() != sndFileMod.channels()) {
        cerr << "Error: files must have the same number of channels\n";
        return 1;
    }

	int channels = sndFileOg.channels();
	vector<short> samplesOg(FRAMES_BUFFER_SIZE * sndFileOg.channels());
	vector<short> samplesMod(FRAMES_BUFFER_SIZE * sndFileMod.channels());
	vector<double> mse(channels, 0.0);
	vector<double> maxAbsError(channels, 0.0);
	vector<double> signalPower(channels, 0.0);
	vector<double> noisePower(channels, 0.0);
	size_t samples = 0;
	size_t nFramesOg;
	size_t nFramesMod;

	while((nFramesOg = sndFileOg.readf(samplesOg.data(), FRAMES_BUFFER_SIZE))) {
		nFramesMod = sndFileMod.readf(samplesMod.data(), FRAMES_BUFFER_SIZE);
		
		for (size_t i = 0; i < nFramesOg; ++i) {
        	for (int n = 0; n < channels; ++n) {
                size_t idx = i * channels + n;
				double ogSamp = samplesOg[idx];
				double modSamp = samplesMod[idx];
                double diff = ogSamp - modSamp;
                mse[n] += diff * diff;
				double absDiff = std::abs(diff);
        		if (absDiff > maxAbsError[n])
            		maxAbsError[n] = absDiff;
				signalPower[n] += ogSamp * ogSamp;
            	noisePower[n] += diff * diff;
            }
        }
		samples += nFramesOg;
	}

	for (int n = 0; n < channels; ++n)
        cout << "Channel " << n << " MSE: " << mse[n] / samples << endl;
	double avgMSE = 0.0;
    for (int n = 0; n < channels; ++n)
        avgMSE += mse[n] / samples;
    avgMSE /= channels;
    cout << "Average MSE: " << avgMSE << "\n\n";

	for (int n = 0; n < channels; ++n)
    	cout << "Channel " << n << " Max Abs Error (L∞): " << maxAbsError[n] << endl;
	double avgMaxAbsError = 0.0;
	for (int n = 0; n < channels; ++n)
		avgMaxAbsError += maxAbsError[n];
	avgMaxAbsError /= channels;
	cout << "Average Max Abs Error (L∞): " << avgMaxAbsError << "\n\n";

	for (int n = 0; n < channels; ++n) {
		double snr = 10.0 * log10(signalPower[n] / (noisePower[n] + 1e-12));
		cout << "Channel " << n << " SNR: " << snr << " dB" << endl;
	}
	double avgSignalPower = 0.0, avgNoisePower = 0.0;
	for (int n = 0; n < channels; ++n) {
		avgSignalPower += signalPower[n] / channels;
		avgNoisePower += noisePower[n] / channels;
	}
	double avgSNR = 10.0 * log10(avgSignalPower / (avgNoisePower + 1e-12));
	cout << "Average SNR: " << avgSNR << " dB" << endl;
}

