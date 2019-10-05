#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>

void create_proc(int sleep_time) { 
    
    char* cmd[] = {"sleep", "-l", (char)(sleep_time), NULL};
    if (fork() == 0)      {
        execv("/bin/sleep", cmd);
        printf("return %d\n", 0);
    } else
        printf("");
} 
int main(int argc, char *argv[]) 
{ 
    if (argc != 3) {
        char* usage = "usage:\n./proc <n_procs> <sleep_time>\n";
        printf(usage);
        exit(1);
    } else { 
        int n_proc = atoi(argv[1]);
        int sleep_time = atoi(argv[2]);
        int i;
        for (i = 0; i < n_proc; i++) {
            create_proc(sleep_time); 
        }        
    }
    return 0; 
} 