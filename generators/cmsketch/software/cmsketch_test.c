#include <stdio.h>
#include "rocc.h"
// #include "encoding.h"

// int main(void){
//     unsigned long long Numin [10] = {
//         1,2,3,4,5,6,7,8,9,10
//     };
//     unsigned long long Numrd = 1;
//     unsigned long long dataout[10] = {
//         0,0,0,0,0,0,0,0,0,0
//     };
//     // start = rdcycle();
//     for(int i=0;i<10;i++){
//         asm volatile ("fence");
//         ROCC_INSTRUCTION_DSS(1,dataout[i],Numin[i],Numrd,3);
//         asm volatile ("fence":::"memory");
//     }
//     // end = rdcycle()
//     return 0;
// }

int main(void){
    unsigned long long Numin [10] = {
        1,2,3,4,5,6,7,8,9,10
    };
    unsigned long long Numrd = 1;
    unsigned long long dataout[100] = {
        1
    };
    // start = rdcycle();
    for(unsigned long long i=0;i<100;i++){
        ROCC_INSTRUCTION_DSS (1,dataout[i],i,Numrd,3);
    };
    return 0;
}