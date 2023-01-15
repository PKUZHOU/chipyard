#include "rocc.h"

static inline void clflush(unsigned long flush_addr)
{
	ROCC_INSTRUCTION_S(0, flush_addr, 0);
}

int main(void)
{
    int data[1024];
	unsigned long long flush_addr = &data[1000];
    data[1000] = 1;
    clflush(flush_addr);
	return 0;
}