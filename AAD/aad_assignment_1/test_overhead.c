#include <stdio.h>
#include <time.h>
#include "aad_data_types.h"

int main() {
    volatile unsigned long long temp = 123456789ULL;
    volatile u08_t result;
    
    clock_t start = clock();
    for(int i = 0; i < 100000000; i++) {
        result = (u08_t)(32 + (temp % 95));
        temp = temp / 95 + i;
    }
    clock_t end = clock();
    
    printf("100M divisions took %.3f seconds\n", (double)(end-start)/CLOCKS_PER_SEC);
    printf("That's %.1f ns per division\n", (double)(end-start)/CLOCKS_PER_SEC * 10);
    return 0;
}
