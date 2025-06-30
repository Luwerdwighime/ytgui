cmake_minimum_required(VERSION 3.10.2)
project("ytgui")
add_library(ytgui SHARED src/main/cpp/ytgui.cpp)
find_library(log-lib log)
find_library(android-lib android)
target_link_libraries(ytgui ${log-lib} ${android-lib})

