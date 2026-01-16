//
// Tomás Oliveira e Silva,  September 2025
//
// Arquiteturas de Alto Desempenho 2025/2026
//


//
// sha1_cuda_kernel() --- each CUDA thread computes the SHA1 secure hash of one message
//
// this kernel should only be used to validate the SHA1 secure hash code in CUDA
//

#include "aad_sha1.h"

typedef unsigned int u32_t;

//
// the nvcc compiler stores w[] in registers (constant indices!)
//
// global thread number: n = threadIdx.x + blockDim.x * blockIdx.x
// global warp number: n >> 5
// warp thread number: n & 31 -- the lane
//

extern "C" __global__ __launch_bounds__(RECOMENDED_CUDA_BLOCK_SIZE,1)
void sha1_cuda_kernel(u32_t *interleaved32_data,u32_t *interleaved32_hash)
{
  u32_t n;

  //
  // get the global thread number (to make things easier, only the x dimension is used)
  //
  n = (u32_t)threadIdx.x + (u32_t)blockDim.x * (u32_t)blockIdx.x;
  //
  // adjust data and hash pointers; together with the DATA and HASH macros below, these pointer adjustments ensure that
  // the 32 threads of a warp access consecutive memory addresses; for one warp addresses grow from the left to the
  // right, and then from top to bottom
  //   +----------------------+----------------------+- ... -+----------------------+----------------------+ 
  //   |                      |                      |       |                      |                      |
  //   | data[ 0] for lane  0 | data[ 0] for lane  1 | ..... | data[ 0] for lane 30 | data[ 0] for lane 31 |
  //   |                      |                      |       |                      |                      |
  //   +----------------------+----------------------+- ... -+----------------------+----------------------+ 
  //   |                      |                      |       |                      |                      |
  //   | data[ 1] for lane  0 | data[ 1] for lane  1 | ..... | data[ 1] for lane 30 | data[ 1] for lane 31 |
  //   |                      |                      |       |                      |                      |
  //   +----------------------+----------------------+- ... -+----------------------+----------------------+ 
  //     ...
  //   +----------------------+----------------------+- ... -+----------------------+----------------------+ 
  //   |                      |                      |       |                      |                      |
  //   | data[13] for lane  0 | data[13] for lane  1 | ..... | data[13] for lane 30 | data[13] for lane 31 |
  //   |                      |                      |       |                      |                      |
  //   +----------------------+----------------------+- ... -+----------------------+----------------------+ 
  // this is followed by the data for the next warp
  //   +----------------------+----------------------+- ... -+----------------------+----------------------+ 
  //   |                      |                      |       |                      |                      |
  //   | data[ 0] for lane  0 | data[ 0] for lane  1 | ..... | data[ 0] for lane 30 | data[ 0] for lane 31 |
  //   |                      |                      |       |                      |                      |
  //   +----------------------+----------------------+- ... -+----------------------+----------------------+ 
  //     ...
  // And so on. The interleaved32_data is CONCEPTUALLY organized in the following way
  //   interleaved32_data[number_of_warps] [14]  [32]
  //                     [warp_number]     [idx] [lane]
  // for the same warp number and the same idx, the data for the 32 lanes (warp thread number) are in consecutive addresses
  //
  // the same happens for the interleaved32_hash, but the indices go only from 0 to 4
  //
  interleaved32_data = &interleaved32_data[(n >> 5u) * (32u * 14u) + (n & 31u)];
  interleaved32_hash = &interleaved32_hash[(n >> 5u) * (32u *  5u) + (n & 31u)];
  //
  // compute the SHA1 secure hash
  //
# define T            u32_t
# define C(c)         (c)
# define ROTATE(x,n)  (((x) << (n)) | ((x) >> (32 - (n))))
# define DATA(idx)    interleaved32_data[32u * (idx)]
# define HASH(idx)    interleaved32_hash[32u * (idx)]
  CUSTOM_SHA1_CODE();
# undef T
# undef C
# undef ROTATE
# undef DATA
# undef HASH
}
