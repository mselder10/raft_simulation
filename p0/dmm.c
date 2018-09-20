#include <stdio.h>  // needed for size_t
#include <sys/mman.h> // needed for mmap
#include <assert.h> // needed for asserts
#include "dmm.h"


typedef struct metadata {
  /* size_t is the return type of the sizeof operator. Since the size of an
   * object depends on the architecture and its implementation, size_t is used
   * to represent the maximum size of any object in the particular
   * implementation. size contains the size of the data object or the number of
   * free bytes
   */

  size_t size; 
  struct metadata* next;
  struct metadata* prev; 
} metadata_t;

/* freelist maintains all the blocks which are not in use; freelist is kept
 * sorted to improve coalescing efficiency 
 */

static metadata_t* freelist = NULL;

void* dmalloc(size_t numbytes) {
  /* initialize through mmap call first time */
    if(freelist == NULL) { 			
        if(!dmalloc_init())
        return NULL;
    }
    assert(numbytes > 0);

    numbytes = ALIGN(numbytes);
    metadata_t* block1 = freelist;
    int tot = numbytes + sizeof(metadata_t);

    void* address = NULL;

    while(block1->size < tot && (block1-> next != NULL)){
      block1 = block1 -> next;
    } //find correct loc

    if(block1-> size >= tot){
        address = (void*)(block1) + sizeof(metadata_t);
        if(block1 -> size == tot){
            if(block1->next != NULL){
                block1->next->prev = block1->prev;
            }

            if(block1->prev == NULL){
                freelist = block1 -> next;
            }
            else{
                block1->prev->next = block1->next;
            }
        }
        else{
            metadata_t* block2 = (metadata_t*)(((void*)block1) + tot);
            block2 -> size = block1-> size - tot;
            block1 -> size = tot;
            if(block2 -> size < ALIGN(1) + sizeof(metadata_t)){
                if(block1->next != NULL){
                    block1->next->prev = block1->prev;
                }
                if(block1->prev == NULL){
                    freelist = block1 -> next;
                }
                else{
                    block1->prev->next = block1->next;
                }
                return address;
            }
            block2->prev = block1 -> prev;
            block2->next = block1 -> next;
            if(block2 -> next != NULL){
                block2 -> next -> prev = block2;
            }

            if(block2 -> prev != NULL){
                block2 -> prev -> next = block2;
            }
            else{
                freelist = block2; //first
            }
            
        }
    block1->prev = NULL;
    block1 -> next = NULL;
    }
return address;
} 


void coalesce(){
    metadata_t* block1 = freelist;
    while(block1->next != NULL){
        metadata_t* block2 = (metadata_t*)((void*)block1 + block1->size);
        if(block1->next == block2){
            block1 -> size = block1 -> size + block2 -> size;
            block1 -> next = block1 -> next -> next;
            if(block1 -> next != NULL){
                block1 -> next -> prev = block1;
            }
        }
        else
        {
            block1 = block1 -> next;
        }
    }
}


void dfree(void* ptr) {

    metadata_t* block0 = (metadata_t*) (ptr - sizeof(metadata_t));
    metadata_t* block1 = freelist;

    if(block1 == NULL){
        freelist = block0;
        block0 -> next = NULL;
        block0 -> prev = NULL;
        return;
    }

    while(block1 <= block0){
        if(block1-> next == NULL){
            block1->next = block0;
            block0-> next = NULL;
            block0 -> prev = block1;
            coalesce();
            return;
        }
        block1 = block1 -> next;
    }
    if(block1->prev == NULL){ //first
        freelist = block0;
    }
    else{
        block1 -> prev -> next = block0;
    }
    block0 -> prev = block1 -> prev;
    block0 -> next = block1;
    block1 -> prev = block0;
    coalesce();
    return;
}    
    




bool dmalloc_init() {

  /* Two choices: 
   * 1. Append prologue and epilogue blocks to the start and the
   * end of the freelist 
   *
   * 2. Initialize freelist pointers to NULL
   *
   * Note: We provide the code for 2. Using 1 will help you to tackle the 
   * corner cases succinctly.
   */

  size_t max_bytes = ALIGN(MAX_HEAP_SIZE);
  /* returns heap_region, which is initialized to freelist */
  freelist = (metadata_t*) mmap(NULL, max_bytes, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
  /* Q: Why casting is used? i.e., why (void*)-1? */
  if (freelist == (void *)-1)
    return false;
  freelist->next = NULL;
  freelist->prev = NULL;
  freelist->size = max_bytes-METADATA_T_ALIGNED;
  return true;
}

/* for debugging; can be turned off through -NDEBUG flag*/
void print_freelist() {
  metadata_t *freelist_head = freelist;
  while(freelist_head != NULL) {
    printf("\tFreelist Size:%zd, Head:%p, Prev:%p, Next:%p\t",
	  freelist_head->size,
	  freelist_head,
	  freelist_head->prev,
	  freelist_head->next);
    freelist_head = freelist_head->next;
  }
  printf("\n");
}
