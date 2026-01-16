#include "audio_codec.h"
#include <iostream>

int main(int argc, char** argv){
    if(argc < 3){
        std::cerr << "Usage: audio_encode <input.wav> <output.golb> [-b blockSize] [-i]\n";
        std::cerr << "  -i : use mid-side transform for stereo\n";
        return 1;
    }
    std::string in = argv[1];
    std::string out = argv[2];
    uint32_t blockSize = 1024;
    bool useMS = false;
    for(int i=3;i<argc;++i){
        std::string a = argv[i];
        if(a == "-i") useMS = true;
        if(a == "-b" && i+1 < argc) { blockSize = std::stoul(argv[++i]); }
    }

    try{
        encodeFile(in,out,blockSize,useMS);
        std::cout << "Encoded " << in << " -> " << out << "\n";
    }catch(const std::exception& e){
        std::cerr << "Error: " << e.what() << "\n";
        return 1;
    }
    return 0;
}
