import java.util.Locale;

/** Reads and prints memory information from Linux's /proc/meminfo file. */
public class memory
{
    /** Converts a memory size from KiB to MiB. */
    private static double kibToMib(long kib)
    {
        return (double) kib / 1024.0; // Divides by 1024 to return the value in MiB.
    }

    /** Converts a memory size from KiB to GiB. */
    private static double kibToGib(long kib)
    {
        return (double) kib / (1024.0 * 1024.0); // Divides by 1024 twice to return the value in GiB.
    }

    /** A snapshot of system memory returned in KiB, or -1 when a value is unavailable. */
    public static final class MemoryData
    {
        private final long totalKiB;
        private final long freeKiB;
        private final long availableKiB;
        private final long usedKiB;
        private final double usagePercent;

        private MemoryData(long totalKiB, long freeKiB, long availableKiB,
                           long usedKiB, double usagePercent)
        {
            this.totalKiB = totalKiB;
            this.freeKiB = freeKiB;
            this.availableKiB = availableKiB;
            this.usedKiB = usedKiB;
            this.usagePercent = usagePercent;
        }

        public long getTotalKiB() { return totalKiB; }
        public long getFreeKiB() { return freeKiB; }
        public long getAvailableKiB() { return availableKiB; }
        public long getUsedKiB() { return usedKiB; }
        public double getUsagePercent() { return usagePercent; }
    }

    /** Reads and returns memory values instead of requiring a caller to parse console output. */
    public static MemoryData getMemInfo()
    {
        long total = memInfo.getTotal();
        long free = memInfo.field("MemFree");
        long available = memInfo.getAvailable();
        long used = total >= 0 && available >= 0 ? total - available : -1;
        double percentage = total > 0 && used >= 0 ? (double) used / total * 100.0 : -1.0;
        return new MemoryData(total, free, available, used, percentage);
    }

    public static void main(String[] args)
    {
        MemoryData info = getMemInfo();
        if (info.getTotalKiB() < 0 || info.getFreeKiB() < 0 || info.getAvailableKiB() < 0) {
            System.err.println("Could not read memory information from /proc/meminfo");
            return;
        }

        System.out.printf(Locale.ROOT, "Total Memory: %.2f MiB (%.2f GiB)%n",
                kibToMib(info.getTotalKiB()), kibToGib(info.getTotalKiB()));
        System.out.printf(Locale.ROOT, "Free Memory: %.2f MiB (%.2f GiB)%n",
                kibToMib(info.getFreeKiB()), kibToGib(info.getFreeKiB()));
        System.out.printf(Locale.ROOT, "Available Memory: %.2f MiB (%.2f GiB)%n",
                kibToMib(info.getAvailableKiB()), kibToGib(info.getAvailableKiB()));
        System.out.printf(Locale.ROOT, "Used Memory: %.2f MiB (%.2f GiB)%n",
                kibToMib(info.getUsedKiB()), kibToGib(info.getUsedKiB()));
        System.out.printf(Locale.ROOT, "Memory Usage: %.2f%%%n", info.getUsagePercent());
    }
}
