cmake_minimum_required(VERSION 3.10)

 # SET (CMAKE_C_COMPILER   "/usr/bin/clang")
 # SET (CMAKE_CXX_COMPILER "/usr/bin/clang++")
 # llvm-profdata -fprofile-use=default.profdata -fprofile-generate
 #  set(MYAPP_PGO "-fprofile-generate") 
 #  set(MYAPP_PGO "-fprofile-use=./default.profdata") 
 
#set(MYAPP_PGO "-fprofile-generate=./pgo") 
#set(MYAPP_PGO "-fprofile-use=./pgo") 
if(NOT "${CMAKE_CXX_STANDARD}")
  set(CMAKE_CXX_STANDARD 17)
endif()

set(CMAKE_CXX_FLAGS "-DNDEBUG -O3 -march=native ${MYAPP_PGO}")
set(CMAKE_EXE_LINKER_FLAGS "-Xlinker -Map=output.map ${MYAPP_PGO}")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${MYAPP_PGO}")
set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${MYAPP_PGO}")

#set(CMAKE_BUILD_TYPE Debug)
SET(CMAKE_BUILD_TYPE Relese)

#set(CMAKE_BUILD_TYPE RelWithDebInfo)

project(sim)
#execute_process(COMMAND sbt "runMain Spinal1802.CDP1802Gen" WORKING_DIRECTORY ../../../)
#execute_process(WORKING_DIRECTORY ../../../ COMMAND cp "./DisplayTest.v_toplevel_tft_initParamsRom.bin" "./src/testing/SDL_DisplayTest/")

find_package(verilator HINTS $ENV{VERILATOR_ROOT})
find_package(SDL2 REQUIRED)

include_directories(${SDL2_INCLUDE_DIRS})

# lz4 for Verilator FST trace (isolated symlinks; clean fix is `sudo apt install liblz4-dev`)
include_directories(/home/winston/.local/include)
link_directories(/home/winston/.local/lib)

include_directories(obj_dir ${VERILATOR_ROOT}/include)

add_library(dt_lib SHARED
 ${VERILATOR_ROOT}/include/verilated.cpp
)

#verilate(dt_lib SOURCES ../comx35_test.v)
#verilate(dt_lib SOURCES ./rtl/comx35_test.v INCLUDE_DIRS "../" TRACE_FST VERILATOR_ARGS "-savable" VERILATOR_ARGS "-O3" VERILATOR_ARGS -x-assign fast VERILATOR_ARGS -x-initial fast VERILATOR_ARGS -prof-cfuncs)
verilate(dt_lib SOURCES ./rtl/comx35_test.v INCLUDE_DIRS "../" VERILATOR_ARGS "-savable" VERILATOR_ARGS "-O3" VERILATOR_ARGS -x-assign fast VERILATOR_ARGS -x-initial fast VERILATOR_ARGS -prof-cfuncs)

# Fast (emulated VIS) model: CPU + FDC + keyboard, no video hardware.
add_library(dt_lib_fast SHARED
 ${VERILATOR_ROOT}/include/verilated.cpp
)
verilate(dt_lib_fast SOURCES ./gen/comx35_fast.v INCLUDE_DIRS "../" VERILATOR_ARGS "-O3" VERILATOR_ARGS -x-assign fast VERILATOR_ARGS -x-initial fast)

add_executable(${CMAKE_PROJECT_NAME}
  main.cpp
  sim.cpp
  sim_fast.cpp
  crt_core.c
  crt_ntsc.c
  comx_loader.cpp
)

# set_target_properties(${CMAKE_PROJECT_NAME} PROPERTIES COMPILE_DEFINITIONS TRACE)

target_link_libraries(${CMAKE_PROJECT_NAME} dt_lib dt_lib_fast -lSDL2_ttf ${SDL2_LIBRARIES})
