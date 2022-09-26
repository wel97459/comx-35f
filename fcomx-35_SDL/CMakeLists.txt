cmake_minimum_required(VERSION 3.10)
#set(CMAKE_BUILD_TYPE Debug)
#SET(CMAKE_BUILD_TYPE Relese)
set(CMAKE_CXX_FLAGS "-O3")
SET(CMAKE_CXX_STANDARD 11)
set(CMAKE_EXE_LINKER_FLAGS "-Xlinker -Map=output.map" )
set(CMAKE_BUILD_TYPE RelWithDebInfo)

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
verilate(dt_lib SOURCES ../comx35_test.v OPT_GLOBAL "-O3" INCLUDE_DIRS "../" TRACE)

add_executable(${CMAKE_PROJECT_NAME}
  main.cpp
  sim.cpp
)

set_target_properties(${CMAKE_PROJECT_NAME} PROPERTIES COMPILE_DEFINITIONS TRACE)

target_link_libraries(${CMAKE_PROJECT_NAME} dt_lib -lSDL2_ttf ${SDL2_LIBRARIES})
