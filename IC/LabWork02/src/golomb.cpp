#include "golomb.h"
#include <sstream>

Golomb::Golomb(unsigned int m_param, NegativeMode mode): m(m_param), negMode(mode){
    if(m == 0){
        throw std::invalid_argument("Parameter m must be greater than 0");
    }
    
    b = static_cast<unsigned int>(std::ceil(std::log2(m)));
    two_power_b = 1 << b;
    cutoff = two_power_b - m;
}

void Golomb::setM(unsigned int new_m){
    if(new_m == 0){
        throw std::invalid_argument("Parameter m must be greater than 0");
    }
    m = new_m;
    b = static_cast<unsigned int>(std::ceil(std::log2(m)));
    two_power_b = 1 << b;
    cutoff = two_power_b - m;
}

unsigned int Golomb::mapToUnsigned(int n) const{
    if(negMode == NegativeMode::SIGN_MAGNITUDE){
        return static_cast<unsigned int>(std::abs(n));
    }else{
        if(n >= 0) {
            return 2 * n;
        }else{
            return -2 * n - 1;
        }
    }
}

int Golomb::mapToSigned(unsigned int n) const{
    if(negMode == NegativeMode::SIGN_MAGNITUDE){
        return static_cast<int>(n);
    }else{
        if(n % 2 == 0){
            return n / 2;
        }else{
            return -(static_cast<int>(n + 1) / 2);
        }
    }
}

std::vector<bool> Golomb::encode(int n) const{
    std::vector<bool> result;
    if(negMode == NegativeMode::SIGN_MAGNITUDE){
        result.push_back(n < 0);
    }
    
    unsigned int mapped = mapToUnsigned(n);
    unsigned int q = mapped / m;
    unsigned int r = mapped % m;

    for(unsigned int i = 0; i < q; i++){
        result.push_back(false);
    }
    result.push_back(true);
    
    if(r < cutoff){
        for(int i = b - 2; i >= 0; i--){
            result.push_back((r >> i) & 1);
        }
    }else{
        unsigned int adjusted = r + cutoff;
        for(int i = b - 1; i >= 0; i--){
            result.push_back((adjusted >> i) & 1);
        }
    }
    return result;
}

void Golomb::encodeTo(int n, std::vector<bool>& out) const{
    if(negMode == NegativeMode::SIGN_MAGNITUDE){
        out.push_back(n < 0);
    }
    unsigned int mapped = mapToUnsigned(n);
    unsigned int q = mapped / m;
    unsigned int r = mapped % m;

    for(unsigned int i = 0; i < q; i++){
        out.push_back(false);
    }
    out.push_back(true);

    if(r < cutoff){
        for(int i = b - 2; i >= 0; i--){
            out.push_back((r >> i) & 1);
        }
    }else{
        unsigned int adjusted = r + cutoff;
        for(int i = b - 1; i >= 0; i--){
            out.push_back((adjusted >> i) & 1);
        }
    }
}

Golomb::DecodeResult Golomb::decode(const std::vector<bool>& bits, size_t startPos) const {
    size_t pos = startPos;
    
    if(pos >= bits.size()){
        throw std::runtime_error("Not enough bits to decode");
    }
    
    bool isNegative = false;
    if(negMode == NegativeMode::SIGN_MAGNITUDE){
        if(pos >= bits.size()){
            throw std::runtime_error("Not enough bits for sign");
        }
        isNegative = bits[pos++];
    }
    
    unsigned int q = 0;
    while(pos < bits.size() && !bits[pos]){
        q++;
        pos++;
    }

    if(pos >= bits.size()){
        throw std::runtime_error("Incomplete unary code");
    }
    pos++;
    
    unsigned int r = 0;
    
    if(b == 0){
        r = 0;
    } else {
        if(pos + (b - 1) > bits.size()){
            throw std::runtime_error("Not enough bits for binary part");
        }

        for(unsigned int i = 0; i < b - 1; i++){
            r = (r << 1) | bits[pos++];
        }

        if(r < cutoff){
        }else{
            if(pos >= bits.size()){
                throw std::runtime_error("Not enough bits for binary part");
            }
            r = (r << 1) | bits[pos++];
            r -= cutoff;
        }
    }
    
    unsigned int mapped = q * m + r;
    int value = mapToSigned(mapped);
    
    if(negMode == NegativeMode::SIGN_MAGNITUDE && isNegative){
        value = -value;
    }
    return{value, pos - startPos};
}

std::string Golomb::bitsToString(const std::vector<bool>& bits){
    std::ostringstream oss;
    for(bool bit : bits){
        oss << (bit ? '1' : '0');
    }
    return oss.str();
}