cmake_minimum_required(VERSION 3.18.1)
project("mfkey32v2")

# Build mfkey32v2 shared library
add_library(
    mfkey32v2
    SHARED
    mfkey32v2.c
    crypto01.c
    bucketsort.c
)

# Find Android log library (optional, for debugging)
find_library(
    log-lib
    log
)

# Link libraries
target_link_libraries(
    mfkey32v2
    ${log-lib}
)

# Compiler optimizations for crypto algorithms
set_target_properties(mfkey32v2 PROPERTIES
    C_FLAGS "-O3 -fomit-frame-pointer"
)
