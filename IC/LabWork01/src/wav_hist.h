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

#ifndef WAVHIST_H
#define WAVHIST_H

#include <iostream>
#include <vector>
#include <map>
#include <sndfile.hh>

class WAVHist {
  private:
	std::vector<std::map<short, size_t>> counts;
	std::vector<std::map<short, size_t>> midCounts;
	std::vector<std::map<short, size_t>> sideCounts;

	std::vector<std::map<short, size_t>> coarseCounts;
	std::vector<std::map<short, size_t>> coarseMidCounts;
	std::vector<std::map<short, size_t>> coarseSideCounts;
	int binSize;

  public:
	WAVHist(const SndfileHandle& sfh) {
		counts.resize(sfh.channels());
		midCounts.resize(1);
		sideCounts.resize(1);

		coarseCounts.resize(sfh.channels());
		coarseMidCounts.resize(1);
		coarseSideCounts.resize(1);
	}

	void update(const std::vector<short>& samples) {
		size_t n {0};
		
		for(auto s : samples){
            size_t ch = n % counts.size();
            counts[ch][s]++;

            short coarseValue = (s / binSize) * binSize;
            coarseCounts[ch][coarseValue]++;
            n++;
		}
			
			
		if (counts.size() == 2 && samples.size() >= 2) {
			for (size_t i = 0; i < samples.size() / 2; i++) {
				// MID channel
				short midValue = (samples[2*i] + samples[2*i+1]) / 2;
				midCounts[0][midValue]++;

				short coarseMidValue = (midValue / binSize) * binSize;
				coarseMidCounts[0][coarseMidValue]++;

				// SIDE channel
				short sideValue = (samples[2*i] - samples[2*i+1]) / 2;
				sideCounts[0][sideValue]++;

				short coarseSideValue = (sideValue / binSize) * binSize;
				coarseSideCounts[0][coarseSideValue]++;
			}
		}
	}

	void dump(const size_t channel) const {
		for(auto [value, counter] : counts[channel])
			std::cout << value << '\t' << counter << '\n';
	}

	void dumpMid() const {
		for(auto [value, counter] : midCounts[0])
			std::cout << value << '\t' << counter << '\n';
	}

	void dumpSide() const {
		for(auto [value, counter] : sideCounts[0])
			std::cout << value << '\t' << counter << '\n';
	}

	void dumpCoarse(const size_t channel) const {
		for(auto [value, counter] : coarseCounts[channel])
			std::cout << value << '\t' << counter << '\n';
	}

	void dumpCoarseMid() const {
		for(auto [value, counter] : coarseMidCounts[0])
			std::cout << value << '\t' << counter << '\n';
	}

	void dumpCoarseSide() const {
		for(auto [value, counter] : coarseSideCounts[0])
			std::cout << value << '\t' << counter << '\n';
	}

};

#endif