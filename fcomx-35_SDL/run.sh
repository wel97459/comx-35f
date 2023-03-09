#!/bin/bash
rm Frames/*
./sim
ffmpeg -y -hide_banner -loglevel error -r 29.97 -f image2 -i Frames/Frame%04d.png -c:v libx264 -preset slow  -crf 29.97 -pix_fmt yuv420p test.mp4