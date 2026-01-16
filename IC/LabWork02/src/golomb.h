#ifndef GOLOMB_H
#define GOLOMB_H

#include <vector>
#include <string>
#include <cmath>
#include <stdexcept>

class Golomb{
public:
    enum class NegativeMode{
        SIGN_MAGNITUDE,
        INTERLEAVING
    };

private:
    unsigned int m;
    unsigned int b;
    unsigned int two_power_b;
    unsigned int cutoff;
    NegativeMode negMode;
    unsigned int mapToUnsigned(int n) const;
    int mapToSigned(unsigned int n) const;

public:
    Golomb(unsigned int m_param, NegativeMode mode = NegativeMode::INTERLEAVING);
    unsigned int getM() const{return m;}
    NegativeMode getNegativeMode() const{return negMode;}
    void setM(unsigned int new_m);
    std::vector<bool> encode(int n) const;
    void encodeTo(int n, std::vector<bool>& out) const;
    struct DecodeResult{
        int value;
        size_t bitsConsumed;
    };
    DecodeResult decode(const std::vector<bool>& bits, size_t startPos = 0) const;
    static std::string bitsToString(const std::vector<bool>& bits);
};

#endif