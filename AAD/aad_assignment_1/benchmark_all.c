#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1_cpu.h"

#define BENCHMARK_DURATION 60

unsigned long long benchmark_cpu_search(int duration_seconds)
{
    u32_t data[14];
    u32_t hash[5];
    u08_t *coin = (u08_t *)data;
    unsigned long long nonce = 0ULL;
    unsigned long long attempts = 0ULL;
    
    const char *hdr = "DETI coin 2 ";
    for(int k = 0; k < 12; k++)
        coin[k ^ 3] = (u08_t)hdr[k];
    for(int j = 12; j < 54; j++)
        coin[j ^ 3] = (u08_t)(32 + (j % 94));
    coin[54 ^ 3] = (u08_t)'\n';
    coin[55 ^ 3] = (u08_t)0x80;
    
    time_t start_time = time(NULL);
    
    while(difftime(time(NULL), start_time) < duration_seconds) {
        coin[12 ^ 3] = (u08_t)(32 + (nonce & 0x3F));
        coin[13 ^ 3] = (u08_t)(32 + ((nonce >> 6) & 0x3F));
        
        sha1(data, hash);
        nonce++;
        attempts++;
    }
    
    return attempts;
}

#if defined(__AVX__)
unsigned long long benchmark_avx_search(int duration_seconds)
{
    const int N_LANES = 4;
    u32_t data[14][N_LANES] __attribute__((aligned(32)));
    u32_t hash[5][N_LANES] __attribute__((aligned(32)));
    unsigned long long base_nonce = 0ULL;
    unsigned long long attempts = 0ULL;
    
    for(int lane = 0; lane < N_LANES; lane++) {
        data[0][lane] = 0x44455449u; 
        data[1][lane] = 0x20636F69u; 
        data[2][lane] = 0x6E203220u;
    }
    for(int w = 3; w < 13; w++)
        for(int lane = 0; lane < N_LANES; lane++)
            data[w][lane] = 0x41414141u + lane + w; 
    for(int lane = 0; lane < N_LANES; lane++)
        data[13][lane] = 0x41410A80u;
    
    time_t start_time = time(NULL);
    
    while(difftime(time(NULL), start_time) < duration_seconds) {
        for(int lane = 0; lane < N_LANES; lane++) {
            unsigned long long nonce = base_nonce + lane;
            data[3][lane] = (u32_t)nonce | 0x20202020u; 
        }
        
        sha1_avx((v4si *)data, (v4si *)hash);
        
        base_nonce += N_LANES;
        attempts += N_LANES;
    }
    
    return attempts;
}
#endif

#if defined(__AVX2__)
unsigned long long benchmark_avx2_search(int duration_seconds)
{
    const int N_LANES = 8;
    u32_t data[14][N_LANES] __attribute__((aligned(64)));
    u32_t hash[5][N_LANES] __attribute__((aligned(64)));
    unsigned long long base_nonce = 0ULL;
    unsigned long long attempts = 0ULL;
    
    for(int lane = 0; lane < N_LANES; lane++) {
        data[0][lane] = 0x44455449u;
        data[1][lane] = 0x20636F69u;
        data[2][lane] = 0x6E203220u;
    }
    for(int w = 3; w < 13; w++)
        for(int lane = 0; lane < N_LANES; lane++)
            data[w][lane] = 0x41414141u + lane + w;
    for(int lane = 0; lane < N_LANES; lane++)
        data[13][lane] = 0x41410A80u;
    
    time_t start_time = time(NULL);
    
    while(difftime(time(NULL), start_time) < duration_seconds) {
        for(int lane = 0; lane < N_LANES; lane++) {
            unsigned long long nonce = base_nonce + lane;
            data[3][lane] = (u32_t)nonce | 0x20202020u;
        }
        
        sha1_avx2((v8si *)data, (v8si *)hash);
        
        base_nonce += N_LANES;
        attempts += N_LANES;
    }
    
    return attempts;
}
#endif

#if defined(__AVX512F__)
unsigned long long benchmark_avx512f_search(int duration_seconds)
{
    const int N_LANES = 16;
    u32_t data[14][N_LANES] __attribute__((aligned(64)));
    u32_t hash[5][N_LANES] __attribute__((aligned(64)));
    unsigned long long base_nonce = 0ULL;
    unsigned long long attempts = 0ULL;
    
    for(int lane = 0; lane < N_LANES; lane++) {
        data[0][lane] = 0x44455449u;
        data[1][lane] = 0x20636F69u;
        data[2][lane] = 0x6E203220u;
    }
    for(int w = 3; w < 13; w++)
        for(int lane = 0; lane < N_LANES; lane++)
            data[w][lane] = 0x41414141u + lane + w;
    for(int lane = 0; lane < N_LANES; lane++)
        data[13][lane] = 0x41410A80u;
    
    time_t start_time = time(NULL);
    
    while(difftime(time(NULL), start_time) < duration_seconds) {
        for(int lane = 0; lane < N_LANES; lane++) {
            unsigned long long nonce = base_nonce + lane;
            data[3][lane] = (u32_t)nonce | 0x20202020u;
        }
        
        sha1_avx512f((v16si *)data, (v16si *)hash);
        
        base_nonce += N_LANES;
        attempts += N_LANES;
    }
    
    return attempts;
}
#endif

void get_cpu_info(char *buffer, size_t len)
{
    FILE *fp = fopen("/proc/cpuinfo", "r");
    if(!fp) {
        snprintf(buffer, len, "Unknown CPU");
        return;
    }
    
    char line[256];
    while(fgets(line, sizeof(line), fp)) {
        if(strncmp(line, "model name", 10) == 0) {
            char *p = strchr(line, ':');
            if(p) {
                p += 2;
                char *q = strchr(p, '\n');
                if(q) *q = '\0';
                snprintf(buffer, len, "%s", p);
                fclose(fp);
                return;
            }
        }
    }
    fclose(fp);
    snprintf(buffer, len, "Unknown CPU");
}

int main(int argc, char **argv)
{
    (void)argc; (void)argv;
    
    char cpu_model[256];
    get_cpu_info(cpu_model, sizeof(cpu_model));
    
    printf("\n");
    printf("====================================\n");
    printf("DETI Coin Search - Performance Benchmark\n");
    printf("====================================\n");
    printf("Duration: %d seconds per test\n", BENCHMARK_DURATION);
    printf("CPU Model: %s\n", cpu_model);
    printf("\n");
    
    FILE *fp = fopen("benchmark_results.csv", "w");
    fprintf(fp, "Implementation,Attempts,Attempts/Second,Attempts/Minute\n");
    
    printf("Benchmarking CPU search (no SIMD)...\n");
    unsigned long long cpu_attempts = benchmark_cpu_search(BENCHMARK_DURATION);
    double cpu_per_sec = (double)cpu_attempts / BENCHMARK_DURATION;
    double cpu_per_min = cpu_per_sec * 60.0;
    printf("  Result: %.2e attempts/sec (%.2e attempts/min)\n\n", cpu_per_sec, cpu_per_min);
    fprintf(fp, "CPU_Baseline,%.0f,%.2e,%.2e\n", (double)cpu_attempts, cpu_per_sec, cpu_per_min);
    
#if defined(__AVX__)
    printf("Benchmarking AVX search (4 lanes)...\n");
    unsigned long long avx_attempts = benchmark_avx_search(BENCHMARK_DURATION);
    double avx_per_sec = (double)avx_attempts / BENCHMARK_DURATION;
    double avx_per_min = avx_per_sec * 60.0;
    printf("  Result: %.2e attempts/sec (%.2e attempts/min)\n", avx_per_sec, avx_per_min);
    printf("  Speedup vs CPU: %.2fx\n\n", avx_per_sec / cpu_per_sec);
    fprintf(fp, "AVX,%.0f,%.2e,%.2e\n", (double)avx_attempts, avx_per_sec, avx_per_min);
#endif
    
#if defined(__AVX2__)
    printf("Benchmarking AVX2 search (8 lanes)...\n");
    unsigned long long avx2_attempts = benchmark_avx2_search(BENCHMARK_DURATION);
    double avx2_per_sec = (double)avx2_attempts / BENCHMARK_DURATION;
    double avx2_per_min = avx2_per_sec * 60.0;
    printf("  Result: %.2e attempts/sec (%.2e attempts/min)\n", avx2_per_sec, avx2_per_min);
    printf("  Speedup vs CPU: %.2fx\n\n", avx2_per_sec / cpu_per_sec);
    fprintf(fp, "AVX2,%.0f,%.2e,%.2e\n", (double)avx2_attempts, avx2_per_sec, avx2_per_min);
#endif
    
#if defined(__AVX512F__)
    printf("Benchmarking AVX-512F search (16 lanes)...\n");
    unsigned long long avx512_attempts = benchmark_avx512f_search(BENCHMARK_DURATION);
    double avx512_per_sec = (double)avx512_attempts / BENCHMARK_DURATION;
    double avx512_per_min = avx512_per_sec * 60.0;
    printf("  Result: %.2e attempts/sec (%.2e attempts/min)\n", avx512_per_sec, avx512_per_min);
    printf("  Speedup vs CPU: %.2fx\n\n", avx512_per_sec / cpu_per_sec);
    fprintf(fp, "AVX512F,%.0f,%.2e,%.2e\n", (double)avx512_attempts, avx512_per_sec, avx512_per_min);
#endif
    
    fclose(fp);
    printf("====================================\n");
    printf("Results saved to benchmark_results.csv\n");
    printf("====================================\n\n");
    
    return 0;
}