#!/bin/bash

# This script will run on the host from the workload directory
# (e.g. workloads/example-fed) every time the workload is built.
# It is recommended to call into something like a makefile because
# this script may be called multiple times.
echo "Building cmsketch-tests benchmark"

riscv64-unknown-elf-gcc -fno-common -fno-builtin-printf -specs=htif_nano.specs -c cmsketch_test.c
riscv64-unknown-elf-gcc -static -specs=htif_nano.specs cmsketch_test.o -o cmsketch_test.riscv