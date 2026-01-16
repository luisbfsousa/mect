//
// Tomás Oliveira e Silva,  September 2025
//
// Arquiteturas de Alto Desempenho 2025/2026
//

#include <time.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1_cpu.h"
#include "aad_cuda_utilities.h"

static void test_sha1_cuda(int n_tests)
{
  u32_t n,*interleaved32_data,*interleaved32_hash,data[14],hash[5],good_hash[5];
  double host_to_device_time,kernel_time,device_to_host_time,hashes_per_second;
  cuda_data_t cd;

  if(n_tests <= 0 || n_tests > (1 << 24) || n_tests % RECOMENDED_CUDA_BLOCK_SIZE != 0)
  {
    fprintf(stderr,"test_sha1_cuda(): bad number of tests\n");
    exit(1);
  }
  // initialize
  cd.device_number = 0; // first device
  cd.cubin_file_name = "sha1_cuda_kernel.cubin";
  cd.kernel_name = "sha1_cuda_kernel";
  cd.data_size[0] = (u32_t)n_tests * (u32_t)14 * (u32_t)sizeof(u32_t); // size of the data array 
  cd.data_size[1] = (u32_t)n_tests * (u32_t) 5 * (u32_t)sizeof(u32_t); // size of the hash array
  fprintf(stderr,"test_sha1_cuda(): %.3f MiB bytes for the interleaved32_data[] array\n",(double)cd.data_size[0] / (double)(1 << 20));
  fprintf(stderr,"test_sha1_cuda(): %.3f MiB bytes for the interleaved32_hash[] array\n",(double)cd.data_size[1] / (double)(1 << 20));
  initialize_cuda(&cd);
  interleaved32_data = (u32_t *)cd.host_data[0];
  interleaved32_hash = (u32_t *)cd.host_data[1];
  // random interleaved32_data
  n = cd.data_size[0];
  while(n != 0u)
    ((u08_t *)interleaved32_data)[--n] = random_byte();
  // run SHA1 in the CUDA device
  time_measurement();
  host_to_device_copy(&cd,0); // idx=0 means that the interleaved32_data is copied to the CUDA device
  time_measurement();
  host_to_device_time = wall_time_delta();
  cd.grid_dim_x = (u32_t)n_tests / (u32_t)RECOMENDED_CUDA_BLOCK_SIZE;
  cd.block_dim_x = (u32_t)RECOMENDED_CUDA_BLOCK_SIZE;
  cd.n_kernel_arguments = 2;
  cd.arg[0] = &cd.device_data[0]; // interleaved32_data
  cd.arg[1] = &cd.device_data[1]; // interleaved32_hash
  time_measurement();
  lauch_kernel(&cd);
  time_measurement();
  kernel_time = wall_time_delta();
  time_measurement();
  device_to_host_copy(&cd,1); // idx=1 means that the interleaved32_hash is copied to the host
  time_measurement();
  device_to_host_time = wall_time_delta();
  // test
  for(n = 0;n < n_tests;n++)
  {
    // deinterleave the data and the hash
    // on the CUDA side, the data for each warp is clustered together; what follows must match what is in the CUDA kernel
    // each warp has 32 threads
    int warp_number = n / 32;
    int lane = n % 32;
    for(int idx = 0;idx < 14;idx++)
      data[idx] = interleaved32_data[32 * 14 * warp_number + 32 * idx + lane];
    for(int idx = 0;idx <  5;idx++)
      hash[idx] = interleaved32_hash[32 *  5 * warp_number + 32 * idx + lane];
    // compute the SHA1 secure hahs on the cpu
    sha1(&data[0],&good_hash[0]);
    // compare them
    for(int idx = 0;idx < 5;idx++)
      if(hash[idx] != good_hash[idx])
      {
        fprintf(stderr,"test_sha1_cuda() failed for n=%d\n",n);
        for(idx = 0;idx < 14;idx++)
          fprintf(stderr,"%2d 0x%08X\n",idx,data[idx]);
        fprintf(stderr,"---\n");
        for(idx = 0;idx < 5;idx++)
          fprintf(stderr,"%2d 0x%08X 0x%08X\n",idx,good_hash[idx],hash[idx]);
        exit(1);
      }
  }
  // cleanup
  terminate_cuda(&cd);
  hashes_per_second = (double)n_tests / kernel_time;
  printf("sha1_cuda_kernel() passed (%d test%s, %.0f secure hashes per second)\n",n_tests,(n_tests == 1) ? "" : "s",hashes_per_second);
  printf("  host -> device --- %.6f seconds\n",host_to_device_time);
  printf("  kernel ----------- %.6f seconds\n",kernel_time);
  printf("  device -> host --- %.6f seconds\n",device_to_host_time);
}


//
// main program
//

int main(void)
{
  test_sha1_cuda(128 * 65536);
  return 0;
}
