#!/bin/bash

# This script will run on the host from the workload directory
# (e.g. workloads/example-fed) every time the workload is built.
# It is recommended to call into something like a makefile because
# this script may be called multiple times.
echo "Building firenmp-tests benchmark"

riscv64-unknown-elf-gcc -fno-common -fno-builtin-printf -specs=htif_nano.specs -c firenmp_test.c
riscv64-unknown-elf-gcc -static -specs=htif_nano.specs firenmp_test.o -o firenmp_test.riscv