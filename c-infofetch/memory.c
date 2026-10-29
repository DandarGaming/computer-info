//import libraries
#include <stdio.h> 
#include <string.h>


struct MemoryInfo { //struct to store Memory information
    unsigned long long MemTotal; //stores the total memory size in bytes
    unsigned long long memFree; //stores the free memory size in bytes
    unsigned long long memAvailable; //stores the available memory size in bytes
    unsigned long long memUsed; //stores the used memory size in bytes
    float percentage; // stores the percentage of memory used
};
double kib_to_mib(unsigned long long kib) {
    return (double)kib / 1024.0;//returns it in MiB by dividing the double variable KiB
}

double kib_to_gib(unsigned long long kib) {
    return (double)kib / (1024.0 * 1024.0);//returns it in GiB by diving the double variable KiB
}


int read_memory_info(struct MemoryInfo *info) {
FILE *Memory = fopen("/proc/meminfo", "r"); // File pointer to store the Memory information AND open the file /proc/meminfo in read mode
    
    // Check if the file was opened successfully
    if (Memory == NULL) {
            perror("/proc/meminfo"); //Prints an error message if the file cannot be opened
            return 1;
        }

    char line[512]; // stores the line read from the file(acts like a checker for the file reading)
    info->MemTotal = 0; // Initialize the total memory size to 0
    info->memFree = 0; // Initialize the free memory size to 0
    info->memAvailable = 0; // Initialize the available memory size to 0

    while (fgets(line, sizeof(line), Memory)) { // Read each line from the file /proc/meminfo
        if (strncmp(line, "MemTotal:", 9) == 0) { // Check if the line starts with "MemTotal:"
            sscanf(line + 9, "%llu", &info->MemTotal); // Read the total memory size from the line
        } 
        
        else if (strncmp(line, "MemFree:", 8) == 0) { // Check if the line starts with "MemFree:"
            sscanf(line + 8, "%llu", &info->memFree); // Read the free memory size from the line
        } 
        
        else if (strncmp(line, "MemAvailable:", 13) == 0) { // Check if the line starts with "MemAvailable:"
            sscanf(line + 13, "%llu", &info->memAvailable); // Read the available memory size from the line
        }
    }

    fclose(Memory); // Close the file pointer Memory

    if (info->MemTotal > 0) { // Check if total memory is greater than zero to avoid division by zero
        info->percentage = ((float)(info->MemTotal - info->memAvailable) / info->MemTotal) * 100.0f; // Calculate the percentage of memory used
    } else {
        info->percentage = 0.0f; // Set percentage to zero if total memory is zero
    }
    

    return 0;
}
int main(void) {
    struct MemoryInfo info;
    
    // Reads memory information and checks for errors
    if(read_memory_info(&info) != 0) { // Call the function to read memory information and check for errors
        return 1; // Return 1 if there was an error reading memory information
    }

    printf("Total Memory: %.2f MiB (%.2f GiB)\n",//prints total memory in MiB and GiB
       kib_to_mib(info.MemTotal),//converts it from KiB to MiB
       kib_to_gib(info.MemTotal)); //converts it from KiB to GiB
    
    printf("Free Memory: %.2f MiB (%.2f GiB)\n",//prints free memory in MiB and GiB
       kib_to_mib(info.memFree),//converts it from KiB to MiB
       kib_to_gib(info.memFree));//converts it from KiB to GiB

printf("Available Memory: %.2f MiB (%.2f GiB)\n",
       kib_to_mib(info.memAvailable),//converts it from KiB to MiB
       kib_to_gib(info.memAvailable));//converts it from KiB to GiB

printf("Used Memory: %.2f MiB (%.2f GiB)\n",
       kib_to_mib(info.MemTotal - info.memAvailable),//converts it from KiB to MiB
       kib_to_gib(info.MemTotal - info.memAvailable));//converts it from KiB to GiB

printf("Memory Usage: %.2f%%\n", info.percentage);//outputs the memory usage as a percentage
}
