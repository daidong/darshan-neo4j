#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string.h>
#include <zlib.h>
#include <stdlib.h>
#include <dirent.h>

#include "darshan-logutils.h"
#include "darshan-log-format.h"

typedef struct _file_{
    uint64_t file_id;
    s_file * next_file;
    s_file * global_next_file;
} s_file;

typedef struct _proc_{
    uint64_t job_id;
    uint64_t proc_id;
    int file_number;
    s_file * first_file;
    s_proc * next_proc;
    s_proc * global_next_proc;
} s_proc;

typedef struct _job_{
    uint64_t job_id;
    int rank_number;
    s_job * exes;
    s_proc * first_proc;
    s_job * next_job;
    s_job * global_next_job;
} s_job;

typedef struct _user_{
    uint64_t user_id;
    int job_number;
    s_job * first_job;
    s_user * global_next_user;
} s_user;

s_user *global_user_ptr = NULL;
s_job *global_job_ptr = NULL;
s_proc *global_proc_ptr = NULL;
s_file *global_file_ptr = NULL;

s_job *last_job_ptr = NULL;
s_proc *last_proc_ptr = NULL;

int user_number = 0;
long job_number = 0;
long proc_number = 0;
long rank_number = 0;
long data_obj_number = 0;


void initialize(){
    s_user user_header = {0};
    s_job job_header = {0};
    s_proc proc_header = {0};
    s_file file_header = {0};

    global_user_ptr = &user_header;
    global_job_ptr = &job_header;
    global_proc_ptr = &proc_header;
    global_file_ptr = &file_header;
}

int darshan_translate_file(const char *log_file_path){

    int i = 0;
    darshan_fd logfile_fd;
    struct darshan_job job;
    struct darshan_file cp_file;

    char *token;
    char *save;
    char buffer[DARSHAN_JOB_METADATA_LEN];

    logfile_fd = darshan_log_open(log_file_path, "r");
    if (!logfile_fd){
        fprintf(stderr, "darshan_log_open() failed to open %s\n", log_file_path);
        return -1;
    }

    int ret = darshan_log_get_job(logfile_fd, &job);
    if (ret < 0){
        darshan_log_close(logfile_fd);
        return -1;
    }

    uint64_t user_id = job.uid;
    uint64_t job_id = job.jobid;
    int64_t last_rank = -1;

    s_user * user_ptr = NULL; 
    s_job * job_ptr = NULL;
    s_proc *proc_ptr = NULL;
    s_file *file_ptr = NULL;

    // Now we got a user and a job
    user_ptr = global_user_ptr;
    while (user_ptr->global_next_user != NULL){
        user_ptr = user_ptr->global_next_user;
        if (user_ptr->user_id == user_id){
            job_ptr = user_ptr->first_job;
            while (job_ptr != NULL){
                job_ptr = job_ptr->next_job;
            }
            s_job * new_job = (s_job *)malloc(sizeof(s_job));
            new_job->job_id = job_id;
            new_job->rank_number = 0;
            new_job->first_proc = NULL;
            new_job->next_job = NULL;
            new_job->global_next_job = NULL;
            last_job_ptr = new_job;
            job_ptr = new_job;
            user_ptr->job_number++;
        }
    }

    if (user_ptr->global_next_user == NULL){
        s_user * prev_ptr = user_ptr;

        user_ptr = (s_user *)malloc(sizeof(s_user));
        user_ptr->user_id = user_id;
        user_ptr->job_number = 1;
        user_ptr->first_job = NULL;
        user_ptr->global_next_user = NULL;
        prev_ptr->global_next_user = user_ptr;
        user_number++;

        s_job * new_job = (s_job *)malloc(sizeof(s_job));
        new_job->job_id = job_id;
        new_job->rank_number = 0;
        new_job->first_proc = NULL;
        new_job->next_job = NULL;
        new_job->global_next_job = NULL;
        last_job_ptr = new_job;
        if (global_job_ptr->global_next_job == NULL){
            global_job_ptr->global_next_job = new_job;
        }
        job_ptr = new_job;
        user_ptr->first_job = new_job;
        job_number++;
    }

    char exe_str[32];
    ret = darshan_log_getexe(logfile_fd, exe_str);
    if (ret < 0){
        darshan_log_close(logfile_fd);
        return -1;
    }
    uint64_t obj_id;
    sscanf(exe_str, "%" PRIu64 "", &obj_id);

    file_ptr = global_file_ptr;
    while (file_ptr->global_next_file != NULL){
        file_ptr = global_file_ptr->global_next_file;
        if (file_ptr->file_id == obj_id){
            job_ptr->exes = file_ptr;
            break;
        }
    }
    if (file_ptr->global_next_file == NULL){
        s_file *new_file = (s_file *)malloc(sizeof(s_file));
        new_file->file_id = obj_id;
        new_file->next_file = NULL;
        new_file->global_next_file = NULL;
        file_ptr->global_next_file = new_file;
        job_ptr->exes = new_file;
        data_obj_number++;
    }


    ret = darshan_log_getfile(logfile_fd, &job, &cp_file);
    if (ret <= 0) {
        fprintf(stderr, "ERROR: darshan log get file error\n");
        darshan_log_close(logfile_fd);
        return -1;
    }

    do {
        if (cp_file.rank != -1 && cp_file.rank < last_rank) {
            fprintf(stderr, "ERROR: log file has out of order rank\n");
            return -1;
        }
        if (cp_file.rank != -1)
            last_rank = cp_file.rank;

        //cp.name_suffix -> the file name
        //cp.rank -> which rank access this file
        //cp.counters[] -> CP_POSIX_READS, CP_POSIX_WRITES
        //cp.hash -> the file's hash
        uint64_t file_hash = cp_file.hash;
        int64_t process_id = cp_file.rank;
        char * name_suffix = cp_file.name_suffix;
        int64_t reads = MAX(cp_file.counters[CP_POSIX_READS],
                            cp_file.counters[CP_POSIX_FREADS],
                            cp_file.counters[CP_BYTES_READ]);
        int64_t writes = MAX(cp_file.counters[CP_POSIX_WRITES],
                             cp_file.counters[CP_POSIX_FWRITES],
                             cp_file.counters[CP_BYTES_WRITTEN]);

        proc_ptr = job_ptr->first_proc;
        while (proc_ptr != NULL){
            if (proc_ptr->proc_id == process_id){
                break;
            }
            proc_ptr = proc_ptr->next_proc;
        }
        if (proc_ptr == NULL){
            s_proc * new_proc = (s_proc *) malloc (sizeof(s_proc));
            new_proc->job_id = job_id;
            new_proc->proc_id = process_id;
            new_proc->file_number = 0;
            new_proc->first_file = NULL;
            new_proc->next_proc = NULL;
            new_proc->global_next_proc = NULL;

            proc_ptr = new_proc;
            job_ptr->rank_number++;

            if (global_proc_ptr->global_next_proc == NULL){
                global_proc_ptr->global_next_proc = new_proc;
            else
                last_proc_ptr->global_next_proc = new_proc;
            last_proc_ptr = new_proc;
            proc_number++;
        }

        file_ptr = global_file_ptr;
        while (file_ptr->global_next_file != NULL){
            file_ptr = global_file_ptr->global_next_file;
            if (file_ptr->file_id == file_hash){
                break;
            }
        }

        if (file_ptr->global_next_file == NULL){
            s_file *new_file = (s_file *)malloc(sizeof(s_file));
            new_file->file_id = obj_id;
            new_file->next_file = NULL;
            new_file->global_next_file = NULL;
            file_ptr->global_next_file = new_file;
            file_ptr = new_file;
            data_obj_number++;
        }

        proc_ptr->file_number++;
        s_file fptr = proc_ptr->first_file;
        while (fptr != NULL){
            if (fptr == file_ptr) //already in the link list. This rank re-visits files
                break;
            fptr = fptr->next_file;
        }
        if (fptr == NULL){
            fptr = file_ptr;
        }

    } while ((ret = darshan_log_getfile(logfile_fd, &job, &cp_file)) == 1);

    if (ret < 0) {
        fprintf(stderr, "ERROR: failed to process log file.\n");
    }

    darshan_log_close(logfile_fd);
    return ret;


}

void iterate_dir(char *path){
    struct dirent *ent = NULL;
    DIR *pDir;
    int trans = 100;

    if((pDir = opendir(path)) != NULL){
        while(NULL != (ent = readdir(pDir)) && trans > 0){
            if(ent->d_type == 8){                    // d_type：8-file，4-directory
                printf("process %s\n", ent->d_name);
                darshan_translate_file(ent->d_name);
                trans--;
            }
            else if(ent->d_name[0] != '.'){
                iterate_dir(ent->d_name);
            }
        }
        closedir(pDir);
    }
}

void print_result(){
    printf("user_number = %d;\n
        job_number = %" PRIu64 ";\n
        proc_number = %" PRIu64 ";\n
        rank_number = %" PRIu64 ";\n
        data_obj_number = %" PRIu64 ".\n", user_number, job_number, proc_number, rank_number, data_obj_number);
}
int main(int argc, char **argv){
    iterate_dir(argv[1]);
    return -1;
}
