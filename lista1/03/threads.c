#include <stdio.h>
#include <pthread.h>
#include <unistd.h>
#include <stdlib.h>

void *await_exit(void *arg) {
    sleep(1);
 	pthread_exit(0);
}

int main (int argc, char *argv[]) {
 	printf("argc %d\n", argc); 

	if (argc != 2) {
        exit(1);
    } else {               
        int n_threads = atoi(argv[1]);
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
            printf("foo %ld\n", ret);
        }
    }    
    pthread_exit(NULL);
}