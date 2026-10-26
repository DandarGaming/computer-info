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

    public static void getMemInfo()
    {
        // Reads the total, free, and available memory values from /proc/meminfo.
        long total = memInfo.getTotal();
        long free = memInfo.field("MemFree");
        long available = memInfo.getAvailable();

        // Checks that all required values were read successfully.
        if (total < 0 || free < 0 || available < 0) {
            System.err.println("Could not read memory information from /proc/meminfo");
            System.exit(1); // Returns an error code if reading memory information failed.
            return;
        }

        // Calculates used memory as total memory minus available memory.
        long used = total - available;
        double percentage = total > 0 ? ((double) used / total) * 100.0 : 0.0;

        // Prints each memory size in both MiB and GiB.
        System.out.printf(Locale.ROOT, "Total Memory: %.2f MiB (%.2f GiB)%n",
                kibToMib(total), kibToGib(total));
        System.out.printf(Locale.ROOT, "Free Memory: %.2f MiB (%.2f GiB)%n",
                kibToMib(free), kibToGib(free));
        System.out.printf(Locale.ROOT, "Available Memory: %.2f MiB (%.2f GiB)%n",
                kibToMib(available), kibToGib(available));
        System.out.printf(Locale.ROOT, "Used Memory: %.2f MiB (%.2f GiB)%n",
                kibToMib(used), kibToGib(used));
        System.out.printf(Locale.ROOT, "Memory Usage: %.2f%%%n", percentage);
    }
}