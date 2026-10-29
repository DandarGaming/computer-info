import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads processor information and CPU usage from Linux's /proc files. */
public class processor
{
    private static final int MAX_CPUS = 1024; // Sets the maximum number of CPUs that can be handled.

    /** Stores the total and idle CPU times read from one cpuN line in /proc/stat. */
    private static class CpuTimes
    {
        long total; // Stores the total number of CPU ticks.
        long idle;  // Stores idle ticks, including time waiting for I/O.
    }

    /** Reads the CPU model name from /proc/cpuinfo. */
    private static String readModel()
    {
        String model = cpuInfo.getField("model name");
        return model.isEmpty() ? "Model name unavailable" : model;
    }

    /** Reads the number of CPU cores, or uses the logical CPU count if it is not listed. */
    private static int readCoreCount()
    {
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
    private static List<CpuTimes> readCpuTimes()
    {
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
    private static double cpuUsage(CpuTimes first, CpuTimes second)
    {
        long total = second.total - first.total; // Finds the total CPU time between the two readings.
        long idle = second.idle - first.idle; // Finds the idle CPU time between the two readings.

        if (total == 0) // Checks for zero elapsed ticks to avoid dividing by zero.
            return 0.0;

        return 100.0 * (total - idle) / total; // Returns the CPU usage as a percentage.
    }

    /**
     * Values that a website or another Java class can read without parsing console output.
     * Usage values are percentages from 0.0 to 100.0; unavailable CPU usage is -1.0.
     */
    public static final class ProcessorData
    {
        private final String model;
        private final int coreCount;
        private final double[] cpuUsages;

        private ProcessorData(String model, int coreCount, double[] cpuUsages)
        {
            this.model = model;
            this.coreCount = coreCount;
            this.cpuUsages = cpuUsages.clone();
        }

        public String getModel() { return model; }
        public int getCoreCount() { return coreCount; }
        public int getLogicalCpuCount() { return cpuUsages.length; }

        /** Returns one logical CPU's usage, or -1.0 if its index is invalid. */
        public double getCpuUsage(int cpuIndex)
        {
            return cpuIndex < 0 || cpuIndex >= cpuUsages.length ? -1.0 : cpuUsages[cpuIndex];
        }

        /** Returns a copy so callers cannot change the stored measurements. */
        public double[] getCpuUsages() { return cpuUsages.clone(); }

        /** Returns average logical-CPU usage, or -1.0 when usage could not be read. */
        public double getAverageUsage()
        {
            if (cpuUsages.length == 0)
                return -1.0;
            double total = 0.0;
            for (double usage : cpuUsages)
                total += usage;
            return total / cpuUsages.length;
        }
    }

    /**
     * Reads processor values and returns them for use by a website or Java caller.
     * CPU usage is measured across a one-second interval, so this call takes about a second.
     */
    public static ProcessorData getCpuInfo()
    {
        String model = readModel();
        int cores = readCoreCount();
        List<CpuTimes> first = readCpuTimes();
        if (first.isEmpty())
            return new ProcessorData(model, cores, new double[0]);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProcessorData(model, cores, new double[0]);
        }

        List<CpuTimes> second = readCpuTimes();
        int count = Math.min(first.size(), second.size());
        double[] usages = new double[count];
        for (int i = 0; i < count; i++)
            usages[i] = cpuUsage(first.get(i), second.get(i));

        return new ProcessorData(model, cores, usages);
    }

    /** Alias retained for callers using the more explicit method name. */
    public static ProcessorData getProcessorInfo()
    {
        return getCpuInfo();
    }

    /** Command-line display; applications should call getCpuInfo() for returned data. */
    public static void main(String[] args)
    {
        ProcessorData info = getCpuInfo();
        System.out.println("CPU Model: " + info.getModel());
        System.out.println("CPU Cores: " + info.getCoreCount());
        System.out.println("Number of Logical CPUs: " + info.getLogicalCpuCount());

        if (info.getLogicalCpuCount() == 0) {
            System.out.println("No CPU usage information found.");
            return;
        }

        for (int i = 0; i < info.getLogicalCpuCount(); i++)
            System.out.printf(Locale.ROOT, "CPU %d Usage: %.2f%%%n", i, info.getCpuUsage(i));
        System.out.printf(Locale.ROOT, "Average CPU Usage: %.2f%%%n", info.getAverageUsage());
    }
}

