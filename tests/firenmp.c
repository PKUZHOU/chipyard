#include "rocc.h"

static inline void clflush(unsigned long flush_addr)
{
	ROCC_INSTRUCTION_S(0, flush_addr, 0);
}

// static inline void send_cmd(unsigned long row_addr)
// {
//     ROCC_INSTRUCTION_S(0, row_addr, 3);
// }

// void test_flush()
// {
//     int data[1024];
// 	unsigned long long flush_addr = &data[1000];
//     data[1000] = 1;
//     clflush(flush_addr);
// }

// void test_send_cmd()
// {
//     unsigned long long row_addr = 0xffff;
//     send_cmd(row_addr);
// }

int main(void)
{
    // test_send_cmd();
    int data[1024];
	unsigned long long flush_addr = &data[1000];
    data[1000] = 1;
    clflush(flush_addr);	
    return 0;
}