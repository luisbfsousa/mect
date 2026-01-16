#include "audio_codec.h"
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <cstring>
#include <algorithm>

struct RawHeader{
    char riff[4];
    uint32_t chunkSize;
    char wave[4];
    char fmt[4];
    uint32_t subchunk1Size;
    uint16_t audioFormat;
    uint16_t numChannels;
    uint32_t sampleRate;
    uint32_t byteRate;
    uint16_t blockAlign;
    uint16_t bitsPerSample;
};

AudioBuffer readWav(const std::string& path){
    std::ifstream ifs(path, std::ios::binary);
    if(!ifs) throw std::runtime_error("Cannot open WAV file: " + path);
    RawHeader hdr;
    ifs.read(reinterpret_cast<char*>(&hdr), sizeof(hdr));
    if(std::strncmp(hdr.riff, "RIFF", 4) != 0 || std::strncmp(hdr.wave, "WAVE", 4) != 0){
        throw std::runtime_error("Not a valid WAV file");
    }

    char chunkId[4];
    uint32_t chunkSize;
    while(ifs.read(chunkId, 4)){
        ifs.read(reinterpret_cast<char*>(&chunkSize), 4);
        if(std::strncmp(chunkId, "data", 4) == 0){
            break;
        }
        ifs.seekg(chunkSize, std::ios::cur);
    }

    AudioBuffer buf;
    buf.numChannels = hdr.numChannels;
    buf.sampleRate = hdr.sampleRate;
    buf.bitsPerSample = hdr.bitsPerSample;
    if(hdr.bitsPerSample != 16){
        throw std::runtime_error("Only 16-bit WAV supported");
    }

    size_t numSamples = chunkSize / (hdr.numChannels * (hdr.bitsPerSample/8));
    buf.samples.resize(numSamples * hdr.numChannels);
    for(size_t i = 0; i < numSamples * hdr.numChannels; ++i){
        int16_t s;
        ifs.read(reinterpret_cast<char*>(&s), sizeof(s));
        buf.samples[i] = s;
    }
    return buf;
}

void writeWav(const std::string& path, const AudioBuffer& buf){
    std::ofstream ofs(path, std::ios::binary);
    if(!ofs) throw std::runtime_error("Cannot open WAV for writing: " + path);
    RawHeader hdr;
    std::memcpy(hdr.riff, "RIFF", 4);
    std::memcpy(hdr.wave, "WAVE", 4);
    std::memcpy(hdr.fmt, "fmt ", 4);
    hdr.subchunk1Size = 16;
    hdr.audioFormat = 1;
    hdr.numChannels = buf.numChannels;
    hdr.sampleRate = buf.sampleRate;
    hdr.bitsPerSample = buf.bitsPerSample;
    hdr.byteRate = hdr.sampleRate * hdr.numChannels * hdr.bitsPerSample/8;
    hdr.blockAlign = hdr.numChannels * hdr.bitsPerSample/8;
    uint32_t dataSize = static_cast<uint32_t>(buf.samples.size() * sizeof(int16_t));
    hdr.chunkSize = 36 + dataSize;
    ofs.write(reinterpret_cast<char*>(&hdr), sizeof(hdr));
    ofs.write("data", 4);
    ofs.write(reinterpret_cast<char*>(&dataSize), 4);
    for(size_t i = 0; i < buf.samples.size(); ++i){
        int16_t s = buf.samples[i];
        ofs.write(reinterpret_cast<char*>(&s), sizeof(s));
    }
}

void BitWriter::flushCur(){
    if(curBits == 0) return;
    cur <<= (8 - curBits);
    out.push_back(cur);
    cur = 0; curBits = 0;
}

void BitWriter::writeBit(bool b){
    cur = (cur << 1) | (b ? 1 : 0);
    curBits++;
    if(curBits == 8){
        out.push_back(cur);
        cur = 0; curBits = 0;
    }
}

void BitWriter::writeBits(const std::vector<bool>& bits){
    for(bool b : bits) writeBit(b);
}

bool BitReader::readBit(size_t& pos) const{
    if(pos/8 >= in.size()) throw std::runtime_error("BitReader out of range");
    uint8_t byte = in[pos/8];
    int bitIndex = 7 - (pos % 8);
    return ((byte >> bitIndex) & 1) != 0;
}

bool BitReader::readBitAdvance(size_t& pos) const{
    bool b = readBit(pos);
    pos++;
    return b;
}

std::vector<bool> BitReader::readBits(size_t& pos, size_t n) const{
    std::vector<bool> out;
    out.reserve(n);
    for(size_t i = 0; i < n; ++i){
        out.push_back(readBitAdvance(pos));
    }
    return out;
}

static void writeUint32(std::ofstream& ofs, uint32_t v){ ofs.write(reinterpret_cast<char*>(&v), 4); }
static void writeUint16(std::ofstream& ofs, uint16_t v){ ofs.write(reinterpret_cast<char*>(&v), 2); }

static uint32_t readUint32(std::ifstream& ifs){ uint32_t v; ifs.read(reinterpret_cast<char*>(&v),4); return v; }
static uint16_t readUint16(std::ifstream& ifs){ uint16_t v; ifs.read(reinterpret_cast<char*>(&v),2); return v; }

void encodeFile(const std::string& inWav, const std::string& outFile, uint32_t blockSize, bool useMidSide){
    AudioBuffer buf = readWav(inWav);
    if(buf.bitsPerSample != 16) throw std::runtime_error("Only 16-bit supported");
    uint32_t numFrames = static_cast<uint32_t>(buf.samples.size() / buf.numChannels);
    std::ofstream ofs(outFile, std::ios::binary);
    if(!ofs) throw std::runtime_error("Cannot open output file");
    ofs.write("GOLB",4);
    uint8_t version = 1;
    ofs.write(reinterpret_cast<char*>(&version),1);
    uint8_t transformFlag = useMidSide ? 1 : 0;
    ofs.write(reinterpret_cast<char*>(&transformFlag),1);
    ofs.write(reinterpret_cast<char*>(&buf.numChannels),1);
    writeUint32(ofs, buf.sampleRate);
    writeUint16(ofs, buf.bitsPerSample);
    writeUint32(ofs, numFrames);
    writeUint32(ofs, blockSize);
    size_t framePos = 0;
    while(framePos < numFrames){
        uint32_t thisBlock = std::min<uint32_t>(blockSize, numFrames - framePos);
        std::vector<std::vector<int32_t>> channels(buf.numChannels, std::vector<int32_t>(thisBlock));
        for(uint32_t f=0; f < thisBlock; ++f){
            for(uint16_t ch=0; ch < buf.numChannels; ++ch){
                channels[ch][f] = buf.samples[(framePos+f)*buf.numChannels + ch];
            }
        }

        if(useMidSide && buf.numChannels == 2){
            std::vector<int32_t> M(thisBlock), S(thisBlock);
            for(uint32_t i=0;i<thisBlock;++i){
                int32_t L = channels[0][i];
                int32_t R = channels[1][i];
                M[i] = L + R;
                S[i] = L - R;
            }
            channels[0].swap(M);
            channels[1].swap(S);
        }

        std::vector<std::vector<int32_t>> residuals(buf.numChannels);
        for(uint16_t ch=0; ch < buf.numChannels; ++ch){
            residuals[ch].resize(thisBlock);
            for(uint32_t i=0;i<thisBlock;++i){
                if(i==0){
                    residuals[ch][i] = channels[ch][i];
                }else{
                    residuals[ch][i] = channels[ch][i] - channels[ch][i-1];
                }
            }
        }

        std::vector<unsigned int> candidates = {1,2,4,8,16,32,64,128,256};
        unsigned int bestM = 1;
        size_t bestBits = SIZE_MAX;
        for(unsigned int m : candidates){
            unsigned int b_local = static_cast<unsigned int>(std::ceil(std::log2(m)));
            unsigned int two_power_b_local = 1u << b_local;
            unsigned int cutoff_local = two_power_b_local - m;
            size_t bits = 0;
            for(uint16_t ch=0; ch < buf.numChannels; ++ch){
                for(uint32_t i=1;i<thisBlock;++i){
                    int32_t val = residuals[ch][i];
                    unsigned int mapped;
                    if(val >= 0) mapped = static_cast<unsigned int>(2*val);
                    else mapped = static_cast<unsigned int>(-2*val - 1);
                    unsigned int q = mapped / m;
                    unsigned int r = mapped % m;
                    bits += (q + 1);
                    if(r < cutoff_local) bits += (b_local - 1);
                    else bits += b_local;
                    if(bits >= bestBits) break;
                }
                if(bits >= bestBits) break;
            }
            if(bits < bestBits){ bestBits = bits; bestM = m; }
        }

        writeUint32(ofs, bestM);
        for(uint16_t ch=0; ch < buf.numChannels; ++ch){
            int32_t first = residuals[ch][0];
            ofs.write(reinterpret_cast<char*>(&first), sizeof(first));
        }

        BitWriter bw;
        Golomb g(bestM, Golomb::NegativeMode::INTERLEAVING);
        std::vector<bool> tmpBits;
        tmpBits.reserve(256);
        for(uint16_t ch=0; ch < buf.numChannels; ++ch){
            for(uint32_t i=1;i<thisBlock;++i){
                tmpBits.clear();
                g.encodeTo(static_cast<int>(residuals[ch][i]), tmpBits);
                bw.writeBits(tmpBits);
            }
        }
    uint32_t totalBits = static_cast<uint32_t>(bw.bitCount());
    bw.flushCur();
    uint32_t byteLen = static_cast<uint32_t>(bw.data().size());
    writeUint32(ofs, byteLen);
    writeUint32(ofs, totalBits);
    ofs.write(reinterpret_cast<const char*>(bw.data().data()), bw.data().size());

        framePos += thisBlock;
    }
}

void decodeFile(const std::string& inFile, const std::string& outWav){
    std::ifstream ifs(inFile, std::ios::binary);
    if(!ifs) throw std::runtime_error("Cannot open input file");
    char magic[4]; ifs.read(magic,4);
    if(std::strncmp(magic, "GOLB",4) != 0) throw std::runtime_error("Not a GOLB file");
    uint8_t version; ifs.read(reinterpret_cast<char*>(&version),1);
    uint8_t transformFlag; ifs.read(reinterpret_cast<char*>(&transformFlag),1);
    uint8_t numChannels; ifs.read(reinterpret_cast<char*>(&numChannels),1);
    uint32_t sampleRate = readUint32(ifs);
    uint16_t bitsPerSample = readUint16(ifs);
    uint32_t numFrames = readUint32(ifs);
    uint32_t blockSize = readUint32(ifs);
    AudioBuffer out;
    out.numChannels = numChannels;
    out.sampleRate = sampleRate;
    out.bitsPerSample = bitsPerSample;
    out.samples.resize(static_cast<size_t>(numFrames) * numChannels);
    size_t framePos = 0;
    while(framePos < numFrames){
        uint32_t thisBlock = std::min<uint32_t>(blockSize, numFrames - framePos);
        uint32_t m = readUint32(ifs);
        std::vector<int32_t> firsts(numChannels);
        for(uint8_t ch=0; ch < numChannels; ++ch){ ifs.read(reinterpret_cast<char*>(&firsts[ch]), sizeof(int32_t)); }
        uint32_t byteLen = readUint32(ifs);
        uint32_t totalBits = readUint32(ifs);
        std::vector<uint8_t> data(byteLen);
        ifs.read(reinterpret_cast<char*>(data.data()), byteLen);
        std::vector<bool> bits;
        bits.reserve(totalBits);
        uint32_t pushed = 0;
        for(uint32_t bi = 0; bi < byteLen && pushed < totalBits; ++bi){
            uint8_t byte = data[bi];
            for(int bit = 7; bit >= 0 && pushed < totalBits; --bit){
                bits.push_back(((byte >> bit) & 1) != 0);
                pushed++;
            }
        }

        size_t bitPos = 0;
        Golomb g(m, Golomb::NegativeMode::INTERLEAVING);
        std::vector<std::vector<int32_t>> residuals(numChannels, std::vector<int32_t>(thisBlock));
            for(uint8_t ch=0; ch < numChannels; ++ch){
            residuals[ch][0] = firsts[ch];
            for(uint32_t i=1;i<thisBlock;++i){
                try{
                    auto res = g.decode(bits, bitPos);
                    residuals[ch][i] = res.value;
                    bitPos += res.bitsConsumed;
                }catch(const std::exception& e){
                    uint32_t b_local = static_cast<uint32_t>(std::ceil(std::log2(m)));
                    uint32_t two_power_b_local = 1u << b_local;
                    uint32_t cutoff_local = two_power_b_local - m;
                    std::cerr << "Decode error in block: m=" << m << " b=" << b_local
                              << " cutoff=" << cutoff_local << " totalBits=" << bits.size()
                              << " bitPos=" << bitPos << " exception=" << e.what() << "\n";
                    throw;
                }
            }
        }

        std::vector<std::vector<int32_t>> channels(numChannels, std::vector<int32_t>(thisBlock));
        for(uint8_t ch=0; ch < numChannels; ++ch){
            for(uint32_t i=0;i<thisBlock;++i){
                if(i==0) channels[ch][i] = residuals[ch][i];
                else channels[ch][i] = channels[ch][i-1] + residuals[ch][i];
            }
        }

        if(transformFlag == 1 && numChannels == 2){
            for(uint32_t i=0;i<thisBlock;++i){
                int32_t M = channels[0][i];
                int32_t S = channels[1][i];
                int32_t L = (M + S) / 2;
                int32_t R = (M - S) / 2;
                channels[0][i] = L;
                channels[1][i] = R;
            }
        }

        for(uint32_t f=0; f < thisBlock; ++f){
            for(uint8_t ch=0; ch < numChannels; ++ch){
                int32_t val = channels[ch][f];
                if(val > INT16_MAX) val = INT16_MAX;
                if(val < INT16_MIN) val = INT16_MIN;
                out.samples[(framePos+f)*numChannels + ch] = static_cast<int16_t>(val);
            }
        }
        framePos += thisBlock;
    }
    writeWav(outWav, out);
}