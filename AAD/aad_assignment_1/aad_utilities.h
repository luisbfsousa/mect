//
// Tomás Oliveira e Silva,  September 2025
//
// Arquiteturas de Alto Desempenho 2025/2026
//
// useful functions (all functions are marked with the unused attribute)
// the compiler will not complain if they are not actually used in the code
//

#ifndef AAD_UTILITIES
#define AAD_UTILITIES

//
// measure elapsed and wall times --- requires <time.h>
//
// warning: Linux and macOS only, if clock_gettime() is not available, consider using clock()
//

static struct timespec measured_cpu_time[2],measured_wall_time[2];

__attribute__((unused))
static void time_measurement(void)
{
  measured_cpu_time[0] = measured_cpu_time[1];
  (void)clock_gettime(CLOCK_PROCESS_CPUTIME_ID,&measured_cpu_time[1]);
  measured_wall_time[0] = measured_wall_time[1];
  (void)clock_gettime(CLOCK_MONOTONIC_RAW,&measured_wall_time[1]);
}

__attribute__((unused))
static double cpu_time_delta(void)
{
  return          ((double)measured_cpu_time[1].tv_sec  - (double)measured_cpu_time[0].tv_sec)
       + 1.0e-9 * ((double)measured_cpu_time[1].tv_nsec - (double)measured_cpu_time[0].tv_nsec);
}

__attribute__((unused))
static double wall_time_delta(void)
{
  return          ((double)measured_wall_time[1].tv_sec  - (double)measured_wall_time[0].tv_sec)
       + 1.0e-9 * ((double)measured_wall_time[1].tv_nsec - (double)measured_wall_time[0].tv_nsec);
}


//
// linear congruential pseudo-random number generator with period 2^32
// see, for example, https://en.wikipedia.org/wiki/Linear_congruential_generator
// not good for cryptographic applications, but good enough to generate test data
//
// 
//

__attribute__((unused))
u08_t random_byte(void)
{ // MMIX by Donald Knuth
  static u64_t x = 2836718637216382173ul;

  x = 6364136223846793005ul * x + 1442695040888963407ul;
  return (u08_t)(x >> 43);
}


//
// the end!
//

#endif
