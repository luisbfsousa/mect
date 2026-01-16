cmake_minimum_required(VERSION 3.16)
project(LabWork03)

SET (CMAKE_BUILD_TYPE "Release")
#SET (CMAKE_BUILD_TYPE "Debug")

SET (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -std=c++20")
SET (CMAKE_CXX_FLAGS_RELEASE "-O3")
SET (CMAKE_CXX_FLAGS_DEBUG "-g3 -fsanitize=address")

SET (BASE_DIR ${CMAKE_SOURCE_DIR} )
SET (CMAKE_RUNTIME_OUTPUT_DIRECTORY ${BASE_DIR}/../bin)

add_executable(base_codec base_codec.cpp)
target_link_libraries(base_codec)

add_executable(comp_codec comp_codec.cpp)
target_link_libraries(comp_codec z)

add_executable(final_codec final_codec.cpp)
target_link_libraries(final_codec z)