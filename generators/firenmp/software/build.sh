echo "Building firenmp-tests benchmark"

riscv64-unknown-elf-gcc -fno-common -fno-builtin-printf -specs=htif_nano.specs -c firenmp_test.c
riscv64-unknown-elf-gcc -static -specs=htif_nano.specs firenmp_test.o -o firenmp_test.riscv