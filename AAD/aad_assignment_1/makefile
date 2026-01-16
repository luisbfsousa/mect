#
# Arquiteturas de Alto Desempenho 2025/2026
#
# makefile for the first practical assignment (A1)
#
# makefile automatic variables:
#   $@ is the name of the target
#   $< is the name of the first prerequisite
#   $^ is the list of names of all prerequisites (without duplicates)
#

#
# CUDA installation directory --- /usr/local/cuda or $(CUDA_HOME)
#

CUDA_DIR = /usr/local/cuda


#
# OpenCL installation directory (for a NVidia graphics card, sama as CUDA)
#

OPENCL_DIR = $(CUDA_DIR)


#
# CUDA device architecture
#
#   GeForce GTX 1660 Ti --- sm_75
#   RTX A2000 Ada --------- sm_86
#   RTX A6000 Ada --------- sm_86
#   RTX 4070 -------------- sm_89
#   RTX 3060 Laptop ------- sm_86
#

CUDA_ARCH = sm_86


#
# clean up
#

clean:
	rm -f sha1_tests
	rm -f sha1_cuda_test sha1_cuda_kernel.cubin
	rm -f a.out
	rm -f cpu_search avx_search avx2_search cuda_search simd_openmp_search client server
	# remove any other build artifacts
	rm -f *.o *.cubin *.exe
	# remove wasm build artifacts
	rm -f wasm_search.js wasm_search.wasm wasm_simd_search.js wasm_simd_search.wasm


#
# test the CUSTOM_SHA1_CODE macro
#

sha1_tests:	aad_sha1_cpu_tests.c aad_sha1.h aad_data_types.h aad_utilities.h makefile
	cc -march=native -Wall -Wshadow -Werror -O3 $< -o $@

sha1_cuda_test:	aad_sha1_cuda_test.c sha1_cuda_kernel.cubin aad_sha1.h aad_data_types.h aad_utilities.h aad_cuda_utilities.h makefile
	cc -march=native -Wall -Wshadow -Werror -O3 $< -o $@ -lcuda


#
# compile the CUDA kernels
#

sha1_cuda_kernel.cubin:			aad_sha1_cuda_kernel.cu aad_sha1.h makefile
	nvcc -arch=$(CUDA_ARCH) --compiler-options -O2,-Wall -I$(CUDA_DIR)/include --cubin $< -o $@


# build

cpu_search: cpu_search.c aad_sha1_cpu.h aad_sha1.h aad_data_types.h aad_utilities.h aad_vault.h makefile
	cc -march=native -Wall -Wshadow -Werror -O3 $< -o $@

avx_search: avx_search.c aad_sha1_cpu.h aad_sha1.h aad_data_types.h aad_utilities.h aad_vault.h makefile
	cc -mavx2 -march=native -Wall -Wshadow -Werror -O3 $< -o $@

avx2_search: avx2_search.c aad_sha1_cpu.h aad_sha1.h aad_data_types.h aad_utilities.h aad_vault.h makefile
	cc -mavx2 -march=native -Wall -Wshadow -Werror -O3 $< -o $@

cuda_search: search_cuda.cu vault_wrapper.c aad_sha1_cpu.h aad_sha1.h aad_data_types.h aad_utilities.h aad_vault.h makefile
	nvcc -arch=$(CUDA_ARCH) --compiler-options -Wall,-O3 -I. search_cuda.cu vault_wrapper.c -o $@

simd_openmp_search: simd_openmp_search.c aad_sha1_cpu.h aad_sha1.h aad_data_types.h aad_utilities.h aad_vault.h makefile
	cc -march=native -fopenmp -Wall -Wshadow -Werror -O3 $< -o $@

opencl_search: opencl_search.c aad_sha1.h aad_data_types.h aad_utilities.h aad_vault.h opencl_search_kernel.cl makefile
	cc -march=native -Wall -Wshadow -Werror -O3 $< -o $@ -I$(OPENCL_DIR)/include -L$(OPENCL_DIR)/lib64 -lOpenCL

# distributed server/client
server: server.c aad_sha1_cpu.h aad_sha1.h aad_data_types.h aad_utilities.h aad_distributed.h aad_vault.h makefile
	cc -march=native -pthread -Wall -Wshadow -Werror -O3 $< -o $@

client: client.c aad_sha1_cpu.h aad_sha1.h aad_data_types.h aad_utilities.h aad_distributed.h makefile
	cc -march=native -fopenmp -Wall -Wshadow -Werror -O3 $< -o $@

benchmark_all: benchmark_all.c aad_sha1_cpu.h aad_sha1.h aad_data_types.h aad_utilities.h makefile
	cc -march=native -Wall -Wshadow -Werror -O3 $< -o $@ -lm

avx512_search: avx512_search.c aad_sha1_cpu.h aad_sha1.h aad_data_types.h aad_utilities.h aad_vault.h makefile
	cc -mavx512f -march=native -Wall -Wshadow -Werror -O3 $< -o $@

cuda_histogram: cuda_histogram_analysis.cu vault_wrapper.c aad_sha1_cpu.h aad_sha1.h aad_data_types.h aad_utilities.h aad_vault.h makefile
	nvcc -arch=$(CUDA_ARCH) --compiler-options -Wall,-O3 -I. cuda_histogram_analysis.cu vault_wrapper.c -o $@

run_benchmarks: benchmark_all
	./benchmark_all
	