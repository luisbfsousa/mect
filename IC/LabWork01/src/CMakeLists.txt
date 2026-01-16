cmake_minimum_required(VERSION 3.16)
project(LabWork01)

SET (CMAKE_BUILD_TYPE "Release")
#SET (CMAKE_BUILD_TYPE "Debug")

SET (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -std=c++20")
SET (CMAKE_CXX_FLAGS_RELEASE "-O3")
SET (CMAKE_CXX_FLAGS_DEBUG "-g3 -fsanitize=address")

SET (BASE_DIR ${CMAKE_SOURCE_DIR} )
SET (CMAKE_RUNTIME_OUTPUT_DIRECTORY ${BASE_DIR}/../bin)

add_library(Common OBJECT)

add_executable (wav_cp wav_cp.cpp)
target_link_libraries (wav_cp sndfile)

add_executable (wav_hist wav_hist.cpp)
target_link_libraries (wav_hist sndfile)

add_executable (wav_dct wav_dct.cpp)
target_link_libraries (wav_dct sndfile fftw3)

add_executable( wav_quant wav_quant.cpp)
target_link_libraries( wav_quant sndfile)

add_executable(wav_cmp wav_cmp.cpp)
target_link_libraries(wav_cmp sndfile)

add_executable(wav_effects wav_effects.cpp)
target_link_libraries(wav_effects sndfile)

target_sources(Common PRIVATE bit_stream.cpp byte_stream.cpp)

add_executable (text2bin text2bin.cpp $<TARGET_OBJECTS:Common>)
add_executable (bin2text bin2text.cpp $<TARGET_OBJECTS:Common>)

add_executable(wav_quant_enc wav_quant_enc.cpp $<TARGET_OBJECTS:Common>)
target_link_libraries(wav_quant_enc sndfile)

add_executable(wav_quant_dec wav_quant_dec.cpp $<TARGET_OBJECTS:Common>)
target_link_libraries(wav_quant_dec sndfile)

add_executable(dct_encoder dct_encoder.cpp $<TARGET_OBJECTS:Common>)
target_link_libraries(dct_encoder sndfile fftw3)

add_executable(dct_decoder dct_decoder.cpp $<TARGET_OBJECTS:Common>)
target_link_libraries(dct_decoder sndfile)

add_executable(wav_stereo2mono wav_stereo2mono.cpp)
target_link_libraries(wav_stereo2mono sndfile)