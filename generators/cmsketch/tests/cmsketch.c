#include <stdio.h>
#include "rocc.h"
// #include "encoding.h"

// int main(void){
//     unsigned long long Numin [10] = {
//         4,2,3,4,5,6,7,8,9,10
//     };
//     unsigned long long Numrd = 1;
//     unsigned long long dataout[10];
//     // start = rdcycle();
//     for(int i=0;i<10;i++){
//         asm volatile ("fence");
//         ROCC_INSTRUCTION_DSS(0,dataout[i],Numin[i],Numrd,3);
//         asm volatile ("fence":::"memory");
//     }
//     // end = rdcycle()
// }

int main(void){
    unsigned long long Numin [1] = {
        7
    };
    unsigned long long Numrd = 1;
    unsigned long long dataout[7];
    // start = rdcycle();
    for(int i=0;i<1;i++){
        asm volatile ("fence");
        ROCC_INSTRUCTION_DSS(0,dataout[i],Numin[i],Numrd,3);
        asm volatile ("fence":::"memory");
    }
    // end = rdcycle()
}

