//------------------------------------------------------------------------------
//
// Copyright 2025 University of Aveiro, Portugal, All Rights Reserved.
//
// These programs are supplied free of charge for research purposes only,
// and may not be sold or incorporated into any commercial product. There is
// ABSOLUTELY NO WARRANTY of any sort, nor any undertaking that they are
// fit for ANY PURPOSE WHATSOEVER. Use them at your own risk. If you do
// happen to find a bug, or have modifications to suggest, please report
// the same to Armando J. Pinho, ap@ua.pt. The copyright notice above
// and this statement of conditions must remain an integral part of each
// and every copy made of these files.
//
// Armando J. Pinho (ap@ua.pt)
// IEETA / DETI / University of Aveiro
//
#include <iostream>
#include <vector>
#include <sndfile.hh>
#include "wav_hist.h"

using namespace std;

constexpr size_t FRAMES_BUFFER_SIZE = 65536; // Buffer for reading frames

int main(int argc, char *argv[]) {

	if(argc < 3) {
		cerr << "Usage: " << argv[0] << " <input file> <channel | mid | side>\n";
		return 1;
	}

	SndfileHandle sndFile { argv[argc-2] };
	if(sndFile.error()) {
		cerr << "Error: invalid input file\n";
		return 1;
    }

	if((sndFile.format() & SF_FORMAT_TYPEMASK) != SF_FORMAT_WAV) {
		cerr << "Error: file is not in WAV format\n";
		return 1;
	}

	if((sndFile.format() & SF_FORMAT_SUBMASK) != SF_FORMAT_PCM_16) {
		cerr << "Error: file is not in PCM_16 format\n";
		return 1;
	}

	string mode { argv[argc-1] };
	int channel {};
	if (mode != "mid" && mode != "side") {
		try {
			channel = stoi(argv[argc-1]);
		} catch(exception &e) {
			cerr << "Error: invalid channel requested\n";
			return 1;
		}
		if (channel >= sndFile.channels()) {
			cerr << "Error: invalid channel requested\n";
			return 1;
		}
	}

	int binSize = 1;
	if (argc >= 4) {
		try {
			binSize = stoi(argv[argc-3]);
			if (binSize < 1 || (binSize & (binSize - 1)) != 0) {
				cerr << "Error: binSize must be a power of 2 (1, 2, 4, 8, ...)\n";
				return 1;
			}
		} catch(exception &e) {
			cerr << "Error: invalid binSize\n";
			return 1;
		}
	}

	size_t nFrames;
	vector<short> samples(FRAMES_BUFFER_SIZE * sndFile.channels());
	WAVHist hist { sndFile };
	while((nFrames = sndFile.readf(samples.data(), FRAMES_BUFFER_SIZE))) {
		samples.resize(nFrames * sndFile.channels());
		hist.update(samples);
	}

	if (binSize > 1) {
		if (mode == "mid"){
			hist.dumpCoarseMid();
		} else if (mode == "side"){
			hist.dumpCoarseSide();
		} else {
			hist.dumpCoarse(channel);
		}
	}else{
		if (mode == "mid"){
			hist.dumpMid();
		}else if (mode == "side"){
			hist.dumpSide();
		}else{
			hist.dump(channel);
		}
	}
	

	return 0;
}

