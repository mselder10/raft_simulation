# You can compile with either gcc or g++
# CC = g++
CC = gcc
CFLAGS = -g -I. -Wall -lm -DNDEBUG
# disable the -DNDEBUG flag for the printing the freelist
OPTFLAG = -O2
DEBUGFLAG = -g

all: dmm.o basicdmmtest quick_test test_basic test_coalesce_order test_coalesce_random test_coalesce test_dmalloc test_free test_general test_peak test_stress_time test_stress

dmm.o: dmm.c
	$(CC) $(CFLAGS) -c dmm.c 
basicdmmtest: basicdmmtest.c dmm.o
	$(CC) $(CFLAGS) -o basicdmmtest basicdmmtest.c dmm.o
quick_test: quick_test.c dmm.o
	$(CC) $(CFLAGS) -o quick_test quick_test.c dmm.o
test_basic: test_basic.c dmm.o
	$(CC) $(CFLAGS) -o test_basic test_basic.c dmm.o
test_coalesce_order: test_coalesce_order.c dmm.o
	$(CC) $(CFLAGS) -o test_coalesce_order test_coalesce_order.c dmm.o
test_coalesce_random: test_coalesce_random.c dmm.o
	$(CC) $(CFLAGS) -o test_coalesce_random test_coalesce_random.c dmm.o
test_coalesce: test_coalesce.c dmm.o
	$(CC) $(CFLAGS) -o test_coalesce test_coalesce.c dmm.o
test_dmalloc: test_dmalloc.c dmm.o
	$(CC) $(CFLAGS) -o test_dmalloc test_dmalloc.c dmm.o
test_free: test_free.c dmm.o
	$(CC) $(CFLAGS) -o test_free test_free.c dmm.o
test_general: test_general.c dmm.o
	$(CC) $(CFLAGS) -o test_general test_general.c dmm.o
test_peak: test_peak.c dmm.o
	$(CC) $(CFLAGS) -o test_peak test_peak.c dmm.o
test_stress_time: test_stress_time.c dmm.o
	$(CC) $(CFLAGS) -o test_stress_time test_stress_time.c dmm.o
test_stress: test_stress.c dmm.o
	$(CC) $(CFLAGS) -o test_stress test_stress.c dmm.o
clean:
	rm -f *.o a.out basicdmmtest quick_test test_basic test_coalesce_order test_coalesce_random test_coalesce test_dmalloc test_free test_general test_peak test_stress_time test_stress
	rm -rf *.dSYM

