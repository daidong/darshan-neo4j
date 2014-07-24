#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string.h>
#include <zlib.h>
#include <stdlib.h>

#include "darshan-log-format.h"
#include "darshan-logutils.h"

#define MAX2(a, b) ((a) > (b)?(a):(b))
#define MAX(a, b, c) ((MAX2(a, b)) > (c) ? (MAX2(a, b)) : (c))

int darshan_translate_file(const char *log_file_path, const char *translate_file_path){
    FILE *fp;

    if ((fp = fopen(translate_file_path, "a")) == NULL){
        fprintf(stderr, "error opening translate file\n");
        return -1;
    }

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

    char exe_str[32];
    ret = darshan_log_getexe(logfile_fd, exe_str);
    if (ret < 0){
        darshan_log_close(logfile_fd);
        return -1;
    }
    uint64_t obj_id;
    sscanf(exe_str, "%" PRIu64 "", &obj_id);

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

        fprintf(fp, "" PRIu64 " " PRIu64 " " PRIu64 " "PRId64 " " PRIu64 " %s" PRId64 " " PRId64 "\n",
                user_id, job_id, obj_id, process_id, file_hash, name_suffix, reads, writes);

    } while ((ret = darshan_log_getfile(logfile_fd, &job, &cp_file)) == 1);

    if (ret < 0) {
        fprintf(stderr, "ERROR: failed to process log file.\n");
    }

    darshan_log_close(logfile_fd);
    fclose(fp);
    return ret;


}

int main(int argc, char **argv){
    darshan_translate_file(argv[1], argv[2]);
    return -1;
}
