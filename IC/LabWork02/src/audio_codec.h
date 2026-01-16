#pragma once
#include <cstdint>
#include <string>
#include <vector>
#include "golomb.h"

struct AudioBuffer{
    uint16_t numChannels;
    uint32_t sampleRate;
    uint16_t bitsPerSample;
    std::vector<int16_t> samples;
};

AudioBuffer readWav(const std::string& path);
void writeWav(const std::string& path, const AudioBuffer& buf);

class BitWriter{
public:
    void writeBit(bool b);
    void writeBits(const std::vector<bool>& bits);
    const std::vector<uint8_t>& data() const { return out; }
    size_t bitCount() const { return out.size()*8 + curBits; }
    void flushCur();
private:
    std::vector<uint8_t> out;
    uint8_t cur = 0;
    int curBits = 0;
    size_t bit_pos = 0;
};

class BitReader{
public:
    BitReader() = default;
    BitReader(const std::vector<uint8_t>& d): in(d){}
    bool readBit(size_t& pos) const;
    bool readBitAdvance(size_t& pos) const;
    std::vector<bool> readBits(size_t& pos, size_t n) const;
    const std::vector<uint8_t>& data() const { return in; }
private:
    std::vector<uint8_t> in;
};

void encodeFile(const std::string& inWav, const std::string& outFile, uint32_t blockSize = 1024, bool useMidSide = false);
void decodeFile(const std::string& inFile, const std::string& outWav);
