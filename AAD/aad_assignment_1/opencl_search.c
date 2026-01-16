#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <time.h>
#define CL_TARGET_OPENCL_VERSION 120
#include <CL/cl.h>
#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1_cpu.h"
#include "aad_vault.h"

static volatile sig_atomic_t stop_requested = 0;

static void handle_sigint(int sig)
{
  (void)sig;
  stop_requested = 1;
}

static void check_opencl_error(cl_int err, const char *operation)
{
  if(err != CL_SUCCESS)
  {
    fprintf(stderr, "OpenCL error during %s: %d\n", operation, err);
    exit(EXIT_FAILURE);
  }
}

static char* load_kernel_source(const char *filename, size_t *size)
{
  FILE *fp = fopen(filename, "r");
  if(!fp)
  {
    fprintf(stderr, "Failed to open kernel file: %s\n", filename);
    exit(EXIT_FAILURE);
  }
  
  fseek(fp, 0, SEEK_END);
  size_t file_size = (size_t)ftell(fp);
  rewind(fp);
  
  char *source = (char*)malloc(file_size + 1);
  if(!source)
  {
    fprintf(stderr, "Failed to allocate memory for kernel source\n");
    fclose(fp);
    exit(EXIT_FAILURE);
  }
  
  size_t read_size = fread(source, 1, file_size, fp);
  source[read_size] = '\0';
  fclose(fp);
  
  if(size) *size = file_size;
  return source;
}

static unsigned long long decode_nonce_from_coin(const u08_t *coin_bytes)
{
  unsigned long long value = 0ULL;
  for(int idx = 9; idx >= 0; --idx)
  {
    unsigned char ch = coin_bytes[(44 + idx) ^ 3];
    if(ch < 32 || ch > 126)
      return 0ULL;
    value = value * 95ULL + (unsigned long long)(ch - 32);
  }
  return value;
}

int main(int argc, char **argv)
{
  unsigned long long n_batches = 0ULL;
  const char *custom_string = NULL;

  for (int i = 1; i < argc; i++)
  {
    if (strcmp(argv[i], "-s") == 0)
    {
      if (i + 1 < argc)
      {
        custom_string = argv[++i];
      }
      else
      {
        fprintf(stderr, "Error: -s requires a string argument.\n");
        return 1;
      }
    }
    else
    {
      n_batches = strtoull(argv[i], NULL, 10);
    }
  }
  
  (void)signal(SIGINT, handle_sigint);
  
  cl_platform_id platform;
  cl_device_id device;
  cl_context context;
  cl_command_queue queue;
  cl_program program;
  cl_kernel kernel;
  cl_int err;
  
  err = clGetPlatformIDs(1, &platform, NULL);
  check_opencl_error(err, "clGetPlatformIDs");
  

  err = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 1, &device, NULL);
  if(err != CL_SUCCESS)
  {
    fprintf(stderr, "No GPU found, using CPU\n");
    err = clGetDeviceIDs(platform, CL_DEVICE_TYPE_CPU, 1, &device, NULL);
    check_opencl_error(err, "clGetDeviceIDs");
  }
  
  char device_name[128];
  clGetDeviceInfo(device, CL_DEVICE_NAME, sizeof(device_name), device_name, NULL);
  fprintf(stderr, "Using OpenCL device: %s\n", device_name);
  
  context = clCreateContext(NULL, 1, &device, NULL, NULL, &err);
  check_opencl_error(err, "clCreateContext");
  
#ifdef CL_VERSION_2_0
  queue = clCreateCommandQueueWithProperties(context, device, NULL, &err);
#else
  queue = clCreateCommandQueue(context, device, 0, &err);
#endif
  check_opencl_error(err, "clCreateCommandQueue");
  
  size_t kernel_size;
  char *kernel_source = load_kernel_source("opencl_search_kernel.cl", &kernel_size);
  
  program = clCreateProgramWithSource(context, 1, (const char**)&kernel_source, &kernel_size, &err);
  check_opencl_error(err, "clCreateProgramWithSource");
  
  err = clBuildProgram(program, 1, &device, NULL, NULL, NULL);
  if(err != CL_SUCCESS)
  {
    size_t log_size;
    clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, 0, NULL, &log_size);
    char *log = (char*)malloc(log_size);
    clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, log_size, log, NULL);
    fprintf(stderr, "Kernel compilation error:\n%s\n", log);
    free(log);
    exit(EXIT_FAILURE);
  }
  
  kernel = clCreateKernel(program, "search_coins_kernel", &err);
  check_opencl_error(err, "clCreateKernel");
  
  free(kernel_source);
  
  const size_t local_work_size = 256;
  const size_t coins_per_batch = 1048576;
  const size_t global_work_size = ((coins_per_batch + local_work_size - 1) / local_work_size) * local_work_size;
  const int max_found = 1024;
  
  srand((unsigned int)time(NULL));
  unsigned long long base_nonce = ((unsigned long long)rand() << 32) | (unsigned long long)rand();

  u32_t h_static_template[14];
  for(int i = 0; i < 14; ++i)
    h_static_template[i] = 0u;

  u08_t *template_bytes = (u08_t *)h_static_template;
  const char *hdr = "DETI coin 2 ";
  
  for(int k = 0; k < 12; ++k)
    template_bytes[k ^ 3] = (u08_t)hdr[k];

  for(int j = 12; j < 44; ++j)
    template_bytes[j ^ 3] = (u08_t)(32 + (rand() % 95));

  for(int j = 44; j < 54; ++j)
    template_bytes[j ^ 3] = (u08_t)' ';

  if(custom_string != NULL)
  {
      size_t len = strlen(custom_string);
      if (len > 42) len = 42; 
      
      for(size_t j = 0; j < len; ++j)
      {
          template_bytes[(12 + j) ^ 3] = (u08_t)custom_string[j];
      }
  }

  template_bytes[54 ^ 3] = (u08_t)'\n';
  template_bytes[55 ^ 3] = (u08_t)0x80;

  if (custom_string) fprintf(stderr, "Custom string set: \"%s\"\n", custom_string);

  cl_mem d_static_template = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                            sizeof(h_static_template), h_static_template, &err);
  check_opencl_error(err, "clCreateBuffer static_template");
  
  cl_mem d_found_coins = clCreateBuffer(context, CL_MEM_WRITE_ONLY, 
                                        max_found * 14 * sizeof(u32_t), NULL, &err);
  check_opencl_error(err, "clCreateBuffer found_coins");
  
  cl_mem d_found_count = clCreateBuffer(context, CL_MEM_READ_WRITE, 
                                        sizeof(int), NULL, &err);
  check_opencl_error(err, "clCreateBuffer found_count");
  
  u32_t *h_found_coins = (u32_t*)malloc(max_found * 14 * sizeof(u32_t));
  int h_found_count;
  unsigned long long batches_done = 0ULL;
  unsigned long long total_iterations = 0ULL;
  unsigned long long last_report_iter = 0ULL;
  unsigned long long coins_found = 0ULL;
  double total_elapsed_time = 0.0;
  
  time_measurement();
  
  fprintf(stderr, "Starting OpenCL search with %zu coins per batch, local work size %zu\n",
          coins_per_batch, local_work_size);
  
  while((n_batches == 0ULL || batches_done < n_batches) && !stop_requested)
  {
    h_found_count = 0;
    err = clEnqueueWriteBuffer(queue, d_found_count, CL_FALSE, 0, 
                               sizeof(int), &h_found_count, 0, NULL, NULL);
    check_opencl_error(err, "clEnqueueWriteBuffer found_count");
    
    cl_ulong cl_base_nonce = (cl_ulong)base_nonce;
    cl_ulong cl_num_coins = (cl_ulong)coins_per_batch;
    
    err = clSetKernelArg(kernel, 0, sizeof(cl_ulong), &cl_base_nonce);
    check_opencl_error(err, "clSetKernelArg 0");
    err = clSetKernelArg(kernel, 1, sizeof(cl_ulong), &cl_num_coins);
    check_opencl_error(err, "clSetKernelArg 1");
    err = clSetKernelArg(kernel, 2, sizeof(cl_mem), &d_static_template);
    check_opencl_error(err, "clSetKernelArg 2");
    err = clSetKernelArg(kernel, 3, sizeof(cl_mem), &d_found_coins);
    check_opencl_error(err, "clSetKernelArg 3");
    err = clSetKernelArg(kernel, 4, sizeof(cl_mem), &d_found_count);
    check_opencl_error(err, "clSetKernelArg 4");
    err = clSetKernelArg(kernel, 5, sizeof(int), &max_found);
    check_opencl_error(err, "clSetKernelArg 5");
    
    err = clEnqueueNDRangeKernel(queue, kernel, 1, NULL, &global_work_size, 
                                 &local_work_size, 0, NULL, NULL);
    check_opencl_error(err, "clEnqueueNDRangeKernel");
    
    err = clEnqueueReadBuffer(queue, d_found_count, CL_TRUE, 0, 
                              sizeof(int), &h_found_count, 0, NULL, NULL);
    check_opencl_error(err, "clEnqueueReadBuffer found_count");
    
    if(h_found_count > 0)
    {
      if(h_found_count > max_found)
        h_found_count = max_found;
      
      err = clEnqueueReadBuffer(queue, d_found_coins, CL_TRUE, 0, 
                                h_found_count * 14 * sizeof(u32_t), 
                                h_found_coins, 0, NULL, NULL);
      check_opencl_error(err, "clEnqueueReadBuffer found_coins");
      
      for(int i = 0; i < h_found_count; i++)
      {
        u32_t *coin = &h_found_coins[i * 14];
        u08_t *coin_bytes = (u08_t*)coin;
        
        u32_t hash[5];
        sha1(coin, hash);
        
        unsigned long long decoded_nonce = decode_nonce_from_coin(coin_bytes);
        printf("Found DETI coin: nonce=%llu\n", decoded_nonce);
        printf("Coin Content: \"");
        for(int b = 0; b < 55; b++)
        {
          unsigned char ch = coin_bytes[b ^ 3];
          putchar((ch >= 32 && ch <= 126) ? (int)ch : '.');
        }
        printf("\"\n");
        
        save_coin(coin);
        coins_found++;
      }
      
      save_coin(NULL);
    }
    
    base_nonce += coins_per_batch;
    batches_done++;
    total_iterations += coins_per_batch;

    if((total_iterations & 0xFFFFFFFFULL) == 0ULL)
    {
      time_measurement();
      double delta = wall_time_delta();
      total_elapsed_time += delta;
      double fps = (double)(total_iterations - last_report_iter) / delta;
      last_report_iter = total_iterations;
      
      fprintf(stderr, "Speed: %.2f MH/s (%.2f M/min) | Nonce: %llx\n",
              fps / 1000000.0,
              (fps * 60.0) / 1000000.0,
              (unsigned long long)base_nonce);
    }
  }
  
  save_coin(NULL);
  time_measurement();
  double final_time = wall_time_delta();
  total_elapsed_time += final_time;

  unsigned long long final_total_hashes = total_iterations;
  double avg_hashes_per_sec = (total_elapsed_time > 0.0) ? (double)final_total_hashes / total_elapsed_time : 0.0;
  double avg_hashes_per_min = avg_hashes_per_sec * 60.0;
  double hashes_per_coin = (coins_found > 0ULL) ? (double)final_total_hashes / (double)coins_found : 0.0;

  printf("\n");
  printf("========================================\n");
  printf("Final Summary (OpenCL):\n");
  printf("========================================\n");
  printf("Total coins found:    %llu\n", coins_found);
  printf("Total hashes:         %llu\n", final_total_hashes);
  printf("Total time:           %.2f seconds\n", total_elapsed_time);
  printf("Average speed:        %.2f MH/s\n", avg_hashes_per_sec / 1000000.0);
  printf("Average speed:        %.2f M/min\n", avg_hashes_per_min / 1000000.0);
  if(coins_found > 0ULL)
    printf("Hashes per coin:      %.2f\n", hashes_per_coin);
  else
    printf("Hashes per coin:      N/A (no coins found)\n");
  printf("========================================\n");
  
  free(h_found_coins);
  clReleaseMemObject(d_static_template);
  clReleaseMemObject(d_found_coins);
  clReleaseMemObject(d_found_count);
  clReleaseKernel(kernel);
  clReleaseProgram(program);
  clReleaseCommandQueue(queue);
  clReleaseContext(context);
  
  return 0;
}