#include <stdio.h>
#include <pthread.h>
#include <unistd.h>
#include <stdlib.h>

void *await_exit(void *st) {
    int sleep_time = (int) st;
    sleep(sleep_time);
 	pthread_exit(0);
}

int main (int argc, char *argv[]) {
	if (argc != 3) {
        char* usage = "usage:\n./threads <n_threads> <sleep_time>\n";
        printf(usage);
        exit(1);
    } else {               
        int n_threads = atoi(argv[1]);
        int sleep_time = atoi(argv[2]);
        pthread_t threads[n_threads];
        
        int i;
        for (i=0; i < n_threads; i++) 
        {
            pthread_create(&threads[i], NULL, await_exit, NULL);
        }        

        int j;
        long ret;
        for (j=0; j< n_threads; j++) 
        {
            pthread_join(threads[j], (void**) &ret);
            printf("return %ld\n", ret);
        }
    }    
    pthread_exit(NULL);    
}