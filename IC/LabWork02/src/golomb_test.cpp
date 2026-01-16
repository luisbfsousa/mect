#include "golomb.h"
#include <iostream>
#include <iomanip>

using namespace std;

void testEncoding(Golomb& golomb, int value){
    vector<bool> encoded = golomb.encode(value);
    auto decoded = golomb.decode(encoded);
    cout << "n=" << value << " m=" << golomb.getM() 
         << " bits=" << encoded.size() 
         << " code=" << Golomb::bitsToString(encoded);
    if(decoded.value == value){
        cout << " OK" << endl;
    }else{
        cout << " FAIL (decoded=" << decoded.value << ")" << endl;
    }
}

void testMode(const string& modeName, Golomb::NegativeMode mode){
    cout << "\nTesting " << modeName << " mode, m=5\n" << endl;
    Golomb golomb(5, mode);
    for(int i = 0; i <= 10; i++){
        testEncoding(golomb, i);
    }
    for(int i = -1; i >= -10; i--){
        testEncoding(golomb, i);
    }
}

void testDifferentM(){
    cout << "\nTesting different m values\n" << endl;
    vector<unsigned int> m_values = {2, 3, 4, 8, 16};
    for(unsigned int m : m_values) {
        cout << "\nm = " << m << ":" << endl;
        Golomb golomb(m, Golomb::NegativeMode::INTERLEAVING);
        for(int i = 0; i <= 5; i++){
            testEncoding(golomb, i);
        }
    }
}

void testAdaptiveM(){
    cout << "\nTesting adaptive m" << endl;
    Golomb golomb(4, Golomb::NegativeMode::INTERLEAVING);
    cout << "\nWith m=4:" << endl;
    testEncoding(golomb, 10);
    testEncoding(golomb, 15);
    golomb.setM(8);
    cout << "\nWith m=8:" << endl;
    testEncoding(golomb, 10);
    testEncoding(golomb, 15);
    golomb.setM(2);
    cout << "\nWith m=2:" << endl;
    testEncoding(golomb, 10);
    testEncoding(golomb, 15);
}

int main(){
    cout << "Golomb Tests" << endl;
    
    try{
        testMode("SIGN-MAGNITUDE", Golomb::NegativeMode::SIGN_MAGNITUDE);
        testMode("INTERLEAVING", Golomb::NegativeMode::INTERLEAVING);
        testDifferentM();
        testAdaptiveM();
    }catch(const exception& e) {
        cerr << "\nError: " << e.what() << endl;
        return 1;
    }
    return 0;
}