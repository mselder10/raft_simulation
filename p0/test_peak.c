#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <signal.h>
#include <unistd.h>
#include <math.h>

#include "dmm.h"

#ifdef HAVE_DRAND48
#define RAND() (drand48())
#define SEED(x) (srand48((x)))
#else
#define RAND() ((double)random()/RAND_MAX)
#define SEED(x) (srandom((x)))
#endif

#define RSEED 0 // 1 for non-deterministic seeding
#define MAX_SIZE 50
#define LPCNT 100000
#define TIMEOUT 180 //seconds

typedef struct sizes {
  void *block; /* a block address */
  int size; /* payload for the block */
} sizes_t;

//Test Case 6: check for O(1) implementation - call with varying number of blocks (nblocks) in argument
//Measures execution time to allocate/free memory with (nblocks/2) free blocks
//First it fills heap with specified blocks of random size
//Then it frees every other block
//Finally, it allocates/free memory with 50% probability for each and measures the time for the same
// return 0 if fails and 1 if passes
int test_case7(int nblock, double *t){
	clock_t begin, end;
	//void *ptr[nblock];
  sizes_t sizes[nblock];
	int ind_pool[LPCNT];
	int size_pool[LPCNT];
	int size, i, j;
  float peak;
  int allocated_bytes = 0;


	//fill the heap by allocating #NBLOCKS of random size in [1,MAX_SIZE]
	for (i = 0; i < nblock; i++) {
		size = (int) (RAND() * MAX_SIZE);
		if (size < 1)
			size = 1;
		//ptr[i] = dmalloc(size);
		sizes[i].block = dmalloc(size);
		if (!sizes[i].block) {
			printf("TC6: Error in Init Alloc #%d\n", i);
			return 0; 
		} else {
      sizes[i].size = size;
      allocated_bytes += size;
    }
	}

	//free every other block
	for (i = 0; i < nblock; i = i + 2) {
		if (sizes[i].block) {
      allocated_bytes -= sizes[i].size;
      sizes[i].block = NULL;
      sizes[i].size = 0;
			dfree(sizes[i].block);
    }
		sizes[i].block = NULL;
	}

  printf("Heap usage starts at ~ %d \n", allocated_bytes);
  printf("Running a sequence of %d operations \n", LPCNT);

	//generate LPCNT indexes to allocate/free in range [0,NBLOCK-1] and random size for allocation
	for (i = 0; i < LPCNT; i++) {
    //can generate negative indexes
		ind_pool[i] = (int) (RAND() * nblock - 1);
		size_pool[i] = (int) (RAND() * MAX_SIZE);
		if (size_pool[i] < 1)
			size_pool[i] = 1;
	}

	//if the specified block is allocated free it otherwise allocate it with random size
  peak = allocated_bytes;
	begin = clock();
	for (i = 0; i < LPCNT; i++) {
		j = ind_pool[i];
		if (sizes[j].block) {
      //printf("freeing\n");
			dfree(sizes[j].block);
      allocated_bytes -= sizes[j].size;
			sizes[j].block = NULL;
      sizes[j].size = 0;
		} else {
      //printf("mallocing\n");
			sizes[j].block = dmalloc(size_pool[i]);
      if (sizes[j].block) {
        sizes[j].size = size_pool[i];
        allocated_bytes += size_pool[i];
        if (peak < allocated_bytes) {
          peak = allocated_bytes;
        }
      }
    }
    //compute peak utilization for certain threesholds based on LPCNT
    if (i == 999 || i == 9999 || i == 99999 || i == 499999 || i == 749999 || i == 999999) {
      float p = (peak / MAX_HEAP_SIZE);
      printf("Peak utilization at iteration %d was %0.3f \n", i, p);
    }
	}
	end = clock();
	*t = (double) (end - begin) / CLOCKS_PER_SEC;

	return 1;
}

void sigalrm_handler(int sig){
	printf("Code is taking more than %d seconds. Aborting!\n",TIMEOUT);
	//Format: Success ExecTime
	fprintf(stderr,"0 0\n");
	exit(1);
}


int main(int argc, char** argv) {

  //based on a MAX_SIZE
  int nblock = ceil(MAX_HEAP_SIZE / ALIGN(MAX_SIZE));
  //printf("heap size: %d, max block size: %d, so #block: %d \n", MAX_HEAP_SIZE, ALIGN(MAX_SIZE), nblock);
	double t;

	if (RSEED)
		SEED(time(NULL));

	signal(SIGALRM,sigalrm_handler);
	alarm(TIMEOUT);
	int rc = test_case7(nblock,&t);
	alarm(0);
	
	fprintf(stderr, "TC7: passed! \n");
	fprintf(stderr, "TC7: Success, NBlock, ExecTime\n");
	//Format: Success ExecTime
	fprintf(stderr," %d %d %f\n",rc,nblock,t);
	
	return 0;
}
