#include "audio_codec.h"
#include <iostream>

int main(int argc, char** argv){
    if(argc < 3){
        std::cerr << "Usage: audio_decode <input.golb> <output.wav>\n";
        return 1;
    }
    std::string in = argv[1];
    std::string out = argv[2];
    try{
        decodeFile(in,out);
        std::cout << "Decoded " << in << " -> " << out << "\n";
    }catch(const std::exception& e){
        std::cerr << "Error: " << e.what() << "\n";
        return 1;
    }
    return 0;
}
