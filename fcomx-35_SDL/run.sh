#!/bin/bash
rm Frames/*
./sim
ffmpeg -y -hide_banner -loglevel error -r 29.97 -f image2 -i Frames/Frame%04d.bmp -crf 29.97 test.avi