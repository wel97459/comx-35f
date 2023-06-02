cmake_minimum_required(VERSION 3.10)

  SET (CMAKE_C_COMPILER   "/usr/bin/clang")
  SET (CMAKE_CXX_COMPILER "/usr/bin/clang++")
 #llvm-profdata -fprofile-use=default.profdata -fprofile-generate
 #  set(MYAPP_PGO "-fprofile-generate") 
 #  set(MYAPP_PGO "-fprofile-use=./default.profdata") 
 
#set(MYAPP_PGO "-fprofile-generate=./pgo") 
#set(MYAPP_PGO "-fprofile-use=./pgo") 
if(NOT "${CMAKE_CXX_STANDARD}")
  set(CMAKE_CXX_STANDARD 11)
endif()

set(CMAKE_CXX_FLAGS "-DNDEBUG -O3 -march=native -flto ${MYAPP_PGO}")
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

include_directories(obj_dir /usr/local/share/verilator/include)

add_library(dt_lib SHARED
 /usr/local/share/verilator/include/verilated.cpp
)

#verilate(dt_lib SOURCES ../comx35_test.v)
verilate(dt_lib SOURCES ./gen/comx35_test.v INCLUDE_DIRS "../" TRACE_FST VERILATOR_ARGS "-savable" VERILATOR_ARGS "-O3" VERILATOR_ARGS -x-assign fast VERILATOR_ARGS -x-initial fast VERILATOR_ARGS -prof-cfuncs)

add_executable(${CMAKE_PROJECT_NAME}
  main.cpp
  sim.cpp
  crt_core.c
  crt_ntsc.c
  comx_loader.cpp
)

set_target_properties(${CMAKE_PROJECT_NAME} PROPERTIES COMPILE_DEFINITIONS TRACE)

target_link_libraries(${CMAKE_PROJECT_NAME} dt_lib -lSDL2_ttf ${SDL2_LIBRARIES})
