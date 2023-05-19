#include <stdio.h>
#include "rocc.h"

// static inline void clflush(unsigned long long flush_addr)
// {
// ROCC_INSTRUCTION_S(0, flush_addr, 0);
// }
static inline void send_cmd(unsigned long long row_addr)
{
    ROCC_INSTRUCTION_S(0,row_addr,3);
}
static inline void get_cmd()
{
    ROCC_INSTRUCTION(0,5);
}
void test_send_cmd(unsigned long row_addr)
{
    // unsigned long row_addr = 0xffff;
    send_cmd(row_addr);
}
void test_get_cmd()
{
    // unsigned long row_addr = 0xffff;
    get_cmd();
}

int main(void)
{
    int i;
    for (unsigned long i=0;i<10;i++){
        test_send_cmd(i+1);
    }
    for (unsigned long i=0;i<10;i++){
        test_get_cmd();
    }
    return 0;
}

// void test_flush()
// {
//     int data[1024];
//unsigned long long flush_addr = &data[1000];
//data[1000] = 1;
//clflush(flush_addr);
//}