#include <stdio.h> 
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
//defines that the max number of CPUs that can be handled is 1024
#define MAX_CPUS 1024

struct CpuInfo { //struct to store CPU information
    char model[256]; //model variable to store the CPU model name with a maximum length of 256 characters
    int cores; //cores variable to store the number of CPU cores
};
struct CpuTimes {//struct to store CPU usage information
    unsigned long long total; //total variable to storing very large(long long) non-negative integer values
    unsigned long long idle; //idle variable to storing very large(long long)non-negative integer values
};





int read_cpu_info(struct CpuInfo *info) {
    FILE *Processor = fopen("/proc/cpuinfo", "r"); // File pointer to store the CPU information AND open the file /proc/cpuinfo in read mode

    if (Processor == NULL) {
        perror("/proc/cpuinfo"); //Prints an error message if the file cannot be opened
        return 1;
    }


    char line[512]; // stores the line read from the file(acts like a checker for the file reading)
    int logical_cpus = 0; // counts the number of logical CPUs found within the file from pointer Processor
    info->model[0] = '\0'; // Initialize the model string to an empty string
    info->cores = 0;

    while (fgets(line, sizeof(line), Processor)) {
        if (strncmp(line, "processor", 9) == 0){
            logical_cpus++;
        }

        if (strncmp(line, "model name", 10) == 0 && info->model[0] == '\0') {

            char *colon = strchr(line, ':'); // Find the colon character in the line

            if (colon != NULL) {
                colon++; // Move the pointer to the character after the colon

                while (*colon == ' ' || *colon == '\t') {
                    colon++; // Skip any leading whitespace characters
                }
                colon[strcspn(colon, "\r\n")] = '\0'; // Remove the newline character from the end of the string

                snprintf(info->model, sizeof(info->model), "%s", colon); // Copy the model name to the info->model string
                
            }
        }
            if (strncmp(line, "cpu cores", 9) == 0 && info->cores == 0) {

                char *colon = strchr(line, ':'); // Finds the colon character in the line
                if (colon != NULL) {
                    info->cores = atoi(colon + 1); // Convert the string after the colon to an integer and store it in info->cores
                }
            }
        }
    
    fclose(Processor); // Close the file pointer Processor Garbage collection

    if (info->model[0] =='\0') {
        snprintf(info->model, sizeof(info->model),"Model name unavailable");
    }

        if(info->cores == 0){ // If the number of CPU cores is not found, set it to the number of logical CPUs
            info->cores = logical_cpus;
        }

        return 0;
    }

    int read_cpu_times(struct CpuTimes times[], int *count) {
        FILE *CpuStatusFile = fopen("/proc/stat", "r"); // File pointer to store the CPU usage information AND open the file /proc/stat in read mode


        if (CpuStatusFile == NULL) {
            perror("/proc/stat"); //Prints an error message if the file cannot be opened
            return 1;
        }


        char line[512]; // stores the line read from the file(acts like a checker for the file reading}
        *count = 0; // Initialize the count of logical CPUs to 0

        while (fgets(line, sizeof(line), CpuStatusFile)) {
                if (strncmp(line, "cpu", 3) != 0 ||line[3] < '0' || line[3] > '9') {
                continue;
                }

                if(*count >= MAX_CPUS) { // Check if the count exceeds the maximum number of CPUs
                    fprintf(stderr, "Exceeded maximum number of CPUs (%d)\n", MAX_CPUS); // Print an error message if the maximum is exceeded
                    break; // Exit the loop if the maximum is exceeded
                }

                unsigned long long user, nice, system, idle_time; // Variables to store the CPU times read from the file
                unsigned long long iowait, irq, softirq, steal; // Variables to store the CPU times read from the file
                unsigned long long guest, guest_nice; // Variables to store the CPU times read from the file

                int fields = sscanf(line, "cpu%*d %llu %llu %llu %llu %llu %llu %llu %llu %llu %llu",
                    &user, &nice, &system, &idle_time, &iowait, &irq, &softirq, &steal, &guest, &guest_nice); // Read the CPU times from the line
            
            if (fields < 4) { // Check if the number of fields read is less than 4
                fprintf(stderr, "Failed to parse CPU times from line: %s\n", line); // Print an error message if parsing fails
                continue; // Skip the current iteration and continue with the next line
            }
            times[*count].idle = idle_time + iowait; // Calculate the total idle time for the CPU
            
            times[*count].total = user + nice + system + idle_time + iowait + irq +softirq + steal;

            (*count)++; // Increment the count of logical CPUs found
        }
        fclose(CpuStatusFile); // Close the file pointer CpuStatusFile Garbage collection
        return 0;
    }
    /* Calculate CPU usage */
    float cpu_usage(struct CpuTimes first, struct CpuTimes second) {
        unsigned long long total = second.total - first.total; // Calculate the difference in total CPU time between the two cycles
        unsigned long long idle = second.idle - first.idle; // Calculate the difference in idle CPU time between the two cycles

        if (total == 0) { // Check if the total difference is zero to avoid division by zero
            return 0.0f; // Return 0.0% usage if there is no change in total CPU time
        }

        return 100.0f * (total - idle) / total; // Return the calculated CPU usage percentage
    }
    


int main(void) {
    struct CpuInfo info;
    if (read_cpu_info(&info) != 0) { 
        return 1; // Return an error code if reading fails
    }

    struct CpuTimes first[MAX_CPUS]; // Array to store the CPU times from the first cycle
    struct CpuTimes second[MAX_CPUS]; // Array to store the CPU times from the second
    int first_count, second_count; // Variables to store the number of logical CPUs found in each cycle

    if (read_cpu_times(first, &first_count) != 0) { // Read the CPU times from the first cycle
        return 1; // Return an error code if reading fails
    }


    sleep(1); // Sleep for 1 second to allow for CPU usage calculation

    if (read_cpu_times(second, &second_count) != 0) { // Read the CPU times from the second cycle
        return 1; // Return an error code if reading fails
    }
    printf("CPU Model: %s\n", info.model); // Print the CPU model name
    printf("CPU Cores: %d\n", info.cores); // Print the number of CPU cores
    printf("Number of Logical CPUs: %d\n", second_count); // Prints the number of logical CPUs 

    int count = first_count < second_count ? first_count : second_count; // the question mark(?) and colon(:) act like if else statements logic:if first count is bigger the second, use it as count else use second_count
    
    float total_usage = 0.0f; // Initialize the total CPU usage to 0

    for (int i = 0; i < count; i++) { // Loop through the number of logical CPUs found
        float usage = cpu_usage(first[i], second[i]); // Calculate the CPU usage for each logical CPU

        printf("CPU %d Usage: %.2f%%\n", i, usage); // Print the CPU usage for each logical CPU

        total_usage += usage; // Add the CPU usage to the total usage
    }
    
    if (count > 0) {
        float average_load = total_usage / count; // Calculate the average CPU usage percentage
        printf("Average CPU Usage: %.2f%%\n", average_load); // Print the average CPU usage percentage
    } 
    else {
        printf("No CPU usage information found.\n"); // Print a message if no CPU usage information was found
    }
    return 0;
    }

