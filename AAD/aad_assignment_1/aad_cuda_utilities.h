//
// Tomás Oliveira e Silva,  September 2025
//
// Arquiteturas de Alto Desempenho 2025/2026
//
// CUDA driver API stuff
//

#ifndef AAD_CUDA_UTILITIES
#define AAD_CUDA_UTILITIES

#include <cuda.h>


//
// data type used to store all CUDA related stuff
//

#define MAX_N_ARGUMENTS  4

typedef struct
{
  // input data
  int device_number;             // number of the device to initialize
  char *cubin_file_name;         // name of the cubin file to load (NULL if not needed)
  char *kernel_name;             // name of the CUDA kernel to load (NULL if not needed)
  u32_t data_size[2];            // the number of bytes of the two data arrays to allocate on the host and on the device (0 if not needed)
  // persistent data
  CUdevice     cu_device;        // the device yhandle
  char         device_name[256]; // the device name
  CUcontext    cu_context;       // the device context
  CUmodule     cu_module;        // the loaded cubin file contents
  CUfunction   cu_kernel;        // the pointer to the CUDA kernel
  CUstream     cu_stream;        // the command stream
  void        *host_data[2];     // the pointers to the host data
  CUdeviceptr  device_data[2];   // the pointers to the device data
  // launch kernel data
  unsigned int grid_dim_x;       // the number of grid blocks (in the X dimension, the only one we will use here)
  unsigned int block_dim_x;      // the number of threads in a block (in the X dimension, the only one we will use here, should be equal to RECOMENDED_CUDA_BLOCK_SIZE)
  int n_kernel_arguments;        // number of kernel arguments
  void *arg[MAX_N_ARGUMENTS];    // pointers to the kernel argument data

}
cuda_data_t;


//
// CU_CALL --- macro that should be used to call a CUDA driver API function and to test its return value
//
// it should be used to test the return value of calls such as
//   cuInit(device_number);
//   cuDeviceGet(&cu_device,device_number);
//
// in these cases, f_name is, respectively, cuInit and cuDeviceGet, and args is, respectively,
//   (device_number) and (&cu_device,device_number)
//

#define CU_CALL(f_name,args)                                                                                  \
  do                                                                                                          \
  {                                                                                                           \
    CUresult e = f_name args;                                                                                 \
    if(e != CUDA_SUCCESS)                                                                                     \
    { /* the call failed, terminate the program */                                                            \
      fprintf(stderr,"" # f_name "() returned %s (file %s, line %d)\n",cu_error_string(e),__FILE__,__LINE__); \
      exit(1);                                                                                                \
    }                                                                                                         \
  }                                                                                                           \
  while(0)


//
// terse description of the CUDA error codes (replacement of the error code number by its name)
//

static const char *cu_error_string(CUresult e)
{
  static char error_string[64];
# define CASE(error_code)  case error_code: return "" # error_code;
  switch((int)e)
  { // list of error codes extracted from cuda.h (TODO: /usr/local/cuda-10.2/targets/x86_64-linux/include/CL)
    default: sprintf(error_string,"unknown error code (%d)",(int)e); return(error_string);
    CASE(CUDA_SUCCESS                             );
    CASE(CUDA_ERROR_INVALID_VALUE                 );
    CASE(CUDA_ERROR_OUT_OF_MEMORY                 );
    CASE(CUDA_ERROR_NOT_INITIALIZED               );
    CASE(CUDA_ERROR_DEINITIALIZED                 );
    CASE(CUDA_ERROR_PROFILER_DISABLED             );
    CASE(CUDA_ERROR_PROFILER_NOT_INITIALIZED      );
    CASE(CUDA_ERROR_PROFILER_ALREADY_STARTED      );
    CASE(CUDA_ERROR_PROFILER_ALREADY_STOPPED      );
    CASE(CUDA_ERROR_NO_DEVICE                     );
    CASE(CUDA_ERROR_INVALID_DEVICE                );
    CASE(CUDA_ERROR_INVALID_IMAGE                 );
    CASE(CUDA_ERROR_INVALID_CONTEXT               );
    CASE(CUDA_ERROR_CONTEXT_ALREADY_CURRENT       );
    CASE(CUDA_ERROR_MAP_FAILED                    );
    CASE(CUDA_ERROR_UNMAP_FAILED                  );
    CASE(CUDA_ERROR_ARRAY_IS_MAPPED               );
    CASE(CUDA_ERROR_ALREADY_MAPPED                );
    CASE(CUDA_ERROR_NO_BINARY_FOR_GPU             );
    CASE(CUDA_ERROR_ALREADY_ACQUIRED              );
    CASE(CUDA_ERROR_NOT_MAPPED                    );
    CASE(CUDA_ERROR_NOT_MAPPED_AS_ARRAY           );
    CASE(CUDA_ERROR_NOT_MAPPED_AS_POINTER         );
    CASE(CUDA_ERROR_ECC_UNCORRECTABLE             );
    CASE(CUDA_ERROR_UNSUPPORTED_LIMIT             );
    CASE(CUDA_ERROR_CONTEXT_ALREADY_IN_USE        );
    CASE(CUDA_ERROR_PEER_ACCESS_UNSUPPORTED       );
    CASE(CUDA_ERROR_INVALID_PTX                   );
    CASE(CUDA_ERROR_INVALID_GRAPHICS_CONTEXT      );
    CASE(CUDA_ERROR_NVLINK_UNCORRECTABLE          );
    CASE(CUDA_ERROR_INVALID_SOURCE                );
    CASE(CUDA_ERROR_FILE_NOT_FOUND                );
    CASE(CUDA_ERROR_SHARED_OBJECT_SYMBOL_NOT_FOUND);
    CASE(CUDA_ERROR_SHARED_OBJECT_INIT_FAILED     );
    CASE(CUDA_ERROR_OPERATING_SYSTEM              );
    CASE(CUDA_ERROR_INVALID_HANDLE                );
    CASE(CUDA_ERROR_NOT_FOUND                     );
    CASE(CUDA_ERROR_NOT_READY                     );
    CASE(CUDA_ERROR_ILLEGAL_ADDRESS               );
    CASE(CUDA_ERROR_LAUNCH_OUT_OF_RESOURCES       );
    CASE(CUDA_ERROR_LAUNCH_TIMEOUT                );
    CASE(CUDA_ERROR_LAUNCH_INCOMPATIBLE_TEXTURING );
    CASE(CUDA_ERROR_PEER_ACCESS_ALREADY_ENABLED   );
    CASE(CUDA_ERROR_PEER_ACCESS_NOT_ENABLED       );
    CASE(CUDA_ERROR_PRIMARY_CONTEXT_ACTIVE        );
    CASE(CUDA_ERROR_CONTEXT_IS_DESTROYED          );
    CASE(CUDA_ERROR_ASSERT                        );
    CASE(CUDA_ERROR_TOO_MANY_PEERS                );
    CASE(CUDA_ERROR_HOST_MEMORY_ALREADY_REGISTERED);
    CASE(CUDA_ERROR_HOST_MEMORY_NOT_REGISTERED    );
    CASE(CUDA_ERROR_HARDWARE_STACK_ERROR          );
    CASE(CUDA_ERROR_ILLEGAL_INSTRUCTION           );
    CASE(CUDA_ERROR_MISALIGNED_ADDRESS            );
    CASE(CUDA_ERROR_INVALID_ADDRESS_SPACE         );
    CASE(CUDA_ERROR_INVALID_PC                    );
    CASE(CUDA_ERROR_LAUNCH_FAILED                 );
    CASE(CUDA_ERROR_NOT_PERMITTED                 );
    CASE(CUDA_ERROR_NOT_SUPPORTED                 );
    CASE(CUDA_ERROR_UNKNOWN                       );
  };
# undef CASE
}


//
// synchonize the stream command buffer
//

static void synchronize_cuda(cuda_data_t *cd)
{
  CU_CALL( cuStreamSynchronize , (cd->cu_stream) );
}

//
// initialize the CUDA driver API interface
//
// load a single cubin file, with a single CUDA kernel
// allocate up to two storage areas both on the host and on the device
//

static void initialize_cuda(cuda_data_t *cd)
{
  //
  // initialize the driver API interface
  //
  CU_CALL( cuInit , (0) );
  //
  // open the CUDA device
  //
  CU_CALL( cuDeviceGet , (&cd->cu_device,cd->device_number) );
  //
  // get information about the CUDA device
  //
  CU_CALL( cuDeviceGetName , (cd->device_name,(int)sizeof(cd->device_name) - 1,cd->cu_device) );
  printf("initialize_cuda(): CUDA code running on a %s (device %d, CUDA %u.%u.%u)\n",cd->device_name,cd->device_number,CUDA_VERSION / 1000,(CUDA_VERSION / 10) % 100,CUDA_VERSION % 10);
  //
  // create a context
  //
  CU_CALL( cuCtxCreate , (&cd->cu_context,CU_CTX_SCHED_YIELD,cd->cu_device) );
  CU_CALL( cuCtxSetCacheConfig , (CU_FUNC_CACHE_PREFER_L1) );
  //
  // load precompiled modules
  //
  CU_CALL( cuModuleLoad , (&cd->cu_module,cd->cubin_file_name) );
  //
  // get the kernel function pointers
  //
  CU_CALL( cuModuleGetFunction, (&cd->cu_kernel,cd->cu_module,cd->kernel_name) );
  //
  // create a command stream (we could have used the default stream)
  //
  CU_CALL( cuStreamCreate, (&cd->cu_stream,CU_STREAM_NON_BLOCKING) );
  //
  // allocate host and device memory
  //
  for(int i = 0;i < 2;i++)
    if(cd->data_size[i] > 0u)
    {
      CU_CALL( cuMemAllocHost , ((void **)&cd->host_data[i]  ,(size_t)cd->data_size[i]) );
      CU_CALL( cuMemAlloc     ,          (&cd->device_data[i],(size_t)cd->data_size[i]) );
    }
    else
      cd->host_data[i] = NULL;
  //
  // catch any lingering errors
  //
  synchronize_cuda(cd);
}


//
// terminate the CUDA driver API interface
//

static void terminate_cuda(cuda_data_t *cd)
{
  CU_CALL( cuStreamDestroy, (cd->cu_stream) );
  for(int i = 0;i < 2;i++)
    if(cd->data_size[i] > 0u)
    {
      CU_CALL( cuMemFreeHost , (cd->host_data[i]) );
      CU_CALL( cuMemFree , (cd->device_data[i]) );
    }
  CU_CALL( cuModuleUnload , (cd->cu_module) );
  CU_CALL( cuCtxDestroy , (cd->cu_context) );
}


//
// copy data from the host to the device and from the device to the host
//

static void host_to_device_copy(cuda_data_t *cd,int idx)
{
  if(idx < 0 || idx > 1 || cd->data_size[idx] == 0u)
  {
    fprintf(stderr,"host_to_device_copy(): bad idx\n");
    exit(1);
  }
  CU_CALL( cuMemcpyHtoD , (cd->device_data[idx],(void *)cd->host_data[idx],(size_t)cd->data_size[idx]) );
  synchronize_cuda(cd);
}

static void device_to_host_copy(cuda_data_t *cd,int idx)
{
  if(idx < 0 || idx > 1 || cd->data_size[idx] == 0u)
  {
    fprintf(stderr,"device_to_host_copy(): bad idx\n");
    exit(1);
  }
  CU_CALL( cuMemcpyDtoH , ((void *)cd->host_data[idx],cd->device_data[idx],(size_t)cd->data_size[idx]) );
  synchronize_cuda(cd);
}



//
// launch a CUDA kernel (with 0 bytes of shared memory and no extra options)
//

static void lauch_kernel(cuda_data_t *cd)
{
  if(cd->block_dim_x != (unsigned int)RECOMENDED_CUDA_BLOCK_SIZE)
    fprintf(stderr,"lauch_kernel(): block_dim_x should be equal to %d\n",RECOMENDED_CUDA_BLOCK_SIZE);
  CU_CALL( cuLaunchKernel , (cd->cu_kernel,cd->grid_dim_x,1u,1u,cd->block_dim_x,1u,1u,0u,cd->cu_stream,&cd->arg[0],NULL) );
  synchronize_cuda(cd);
}

#endif
