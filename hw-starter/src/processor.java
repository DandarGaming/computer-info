import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads processor information and CPU usage from Linux's /proc files. */
public class processor {
    private static final int MAX_CPUS = 1024; // Sets the maximum number of CPUs that can be handled.

    /**
     * Stores the total and idle CPU times read from one cpuN line in /proc/stat.
     */
    private static class CpuTimes {
        long total; // Stores the total number of CPU ticks.
        long idle; // Stores idle ticks, including time waiting for I/O.
    }

    /** Reads the CPU model name from /proc/cpuinfo. */
    private static String readModel() 
    {
        String model = cpuInfo.getField("model name");
        return model.isEmpty() ? "Model name unavailable" : model;
    }

    /**
     * Reads the number of CPU cores, or uses the logical CPU count if it is not
     * listed.
     */
    private static int readCoreCount() {
        String value = cpuInfo.getField("cpu cores");
        try {
            int cores = Integer.parseInt(value);
            if (cores > 0)
                return cores;
        } catch (NumberFormatException e) {
            // Uses the logical CPU count below if the core count is missing or invalid.
        }
        return cpuInfo.getCount();
    }

    /** Reads each logical CPU's time values from /proc/stat. */
    private static List<CpuTimes> readCpuTimes() {
        List<CpuTimes> times = new ArrayList<>();

        // Reads each line from /proc/stat and keeps lines named cpu0, cpu1, and so on.
        for (String line : sysFile.lines(sysFile.path("/proc/stat"))) {
            if (!line.startsWith("cpu") || line.length() <= 3 || !Character.isDigit(line.charAt(3)))
                continue; // Skips the aggregate "cpu" line and any non-CPU lines.

            if (times.size() >= MAX_CPUS) { // Checks that the CPU limit has not been exceeded.
                System.err.printf("Exceeded maximum number of CPUs (%d)%n", MAX_CPUS);
                break; // Stops reading once the maximum number of CPUs is reached.
            }

            String[] parts = line.strip().split("\\s+");
            if (parts.length < 5) { // A CPU line needs at least user, nice, system, and idle values.
                System.err.println("Failed to parse CPU times from line: " + line);
                continue; // Skips this line and continues with the next one.
            }

            long[] fields = new long[8]; // Stores user, nice, system, idle, iowait, irq, softirq, and steal.
            try {
                // Reads the available CPU time fields; fields omitted at the end remain zero.
                for (int i = 0; i < fields.length && i + 1 < parts.length; i++)
                    fields[i] = Long.parseLong(parts[i + 1]);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse CPU times from line: " + line);
                continue; // Skips this line and continues with the next one.
            }

            CpuTimes cpu = new CpuTimes();
            cpu.idle = fields[3] + fields[4]; // Adds idle and I/O wait time to get total idle time.
            cpu.total = fields[0] + fields[1] + fields[2] + fields[3]
                    + fields[4] + fields[5] + fields[6] + fields[7];
            times.add(cpu); // Stores the parsed times for this logical CPU.
        }
        return times;
    }

    /** Calculates CPU usage from the difference between two time readings. */
    private static double cpuUsage(CpuTimes first, CpuTimes second) {
        long total = second.total - first.total; // Finds the total CPU time between the two readings.
        long idle = second.idle - first.idle; // Finds the idle CPU time between the two readings.

        if (total == 0) // Checks for zero elapsed ticks to avoid dividing by zero.
            return 0.0;

        return 100.0 * (total - idle) / total; // Returns the CPU usage as a percentage.
    }

    public static void getCpuInfo() {
        String model = readModel(); // Reads the processor model name.
        int cores = readCoreCount(); // Reads the number of CPU cores.

        List<CpuTimes> first = readCpuTimes(); // Reads CPU times for the first measurement.
        if (first.isEmpty()) {
            System.err.println("No CPU usage information found in /proc/stat");
            System.exit(1); // Returns an error code if the CPU times could not be read.
            return;
        }

        try {
            Thread.sleep(1000); // Waits one second so that CPU usage can be calculated.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restores the interrupted status of this thread.
            System.err.println("CPU usage measurement was interrupted");
            System.exit(1);
            return;
        }

        List<CpuTimes> second = readCpuTimes(); // Reads CPU times for the second measurement.
        System.out.println("CPU Model: " + model); // Prints the CPU model name.
        System.out.println("CPU Cores: " + cores); // Prints the number of CPU cores.
        System.out.println("Number of Logical CPUs: " + second.size()); // Prints the logical CPU count.

        int count = Math.min(first.size(), second.size()); // Uses the smaller count from both readings.
        double totalUsage = 0.0; // Initializes the sum of CPU usage percentages to zero.

        for (int i = 0; i < count; i++) { // Loops through the logical CPUs found in both readings.
            double usage = cpuUsage(first.get(i), second.get(i)); // Calculates this CPU's usage.
            System.out.printf(Locale.ROOT, "CPU %d Usage: %.2f%%%n", i, usage);
            totalUsage += usage; // Adds this CPU's usage to the total usage.
        }

        if (count > 0) {
            double averageLoad = totalUsage / count; // Calculates the average CPU usage percentage.
            System.out.printf(Locale.ROOT, "Average CPU Usage: %.2f%%%n", averageLoad);
        } else {
            System.out.println("No CPU usage information found.");
        }
    }
}
