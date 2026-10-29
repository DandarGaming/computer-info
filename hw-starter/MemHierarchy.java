import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Measures memory-access time for several array strides and working-set sizes.
 * Run on Linux with: java -cp out cacheBenchmark
 * The table and estimates are approximate: JVM warmup, CPU frequency scaling,
 * prefetching and virtualization can all affect the results.
 */
public class cacheBenchmark
{
    // Each array element is a double, so these strides correspond to 8 through 1024 bytes.
    private static final int[] STRIDE_BYTES = {8, 16, 32, 64, 128, 256, 512, 1024};
    // Working sets grow from 4 KiB through 64 MiB to cross typical cache capacities.
    private static final int[] WORKING_SET_KIB = {4, 16, 64, 256, 1024, 4096, 16384, 65536};
    private static final long TARGET_ACCESSES = 1_000_000;
    private static final int SAMPLES = 3;

    // A volatile result makes the measured array reads observable to the JVM.
    private static volatile double resultSink;

    public static void main(String[] args)
    {
        double[][] times = new double[WORKING_SET_KIB.length][STRIDE_BYTES.length];
        int maxKiB = WORKING_SET_KIB[WORKING_SET_KIB.length - 1];
        double[] values = new double[maxKiB * 1024 / Double.BYTES];

        // Fill the array once with values that the timed loop must actually read.
        for (int i = 0; i < values.length; i++) {
            values[i] = (i % 101) + 0.25;
        }

        System.out.println("Time per access (nanoseconds; lower is faster)");
        System.out.printf(Locale.ROOT, "%-12s", "Working set");
        for (int stride : STRIDE_BYTES) {
            System.out.printf(Locale.ROOT, "%10s", stride + " B");
        }
        System.out.println();

        // Repeat each working-set measurement for all strides, using the median sample.
        for (int sizeIndex = 0; sizeIndex < WORKING_SET_KIB.length; sizeIndex++) {
            int sizeKiB = WORKING_SET_KIB[sizeIndex];
            int elementCount = sizeKiB * 1024 / Double.BYTES;
            System.out.printf(Locale.ROOT, "%-12s", sizeKiB >= 1024
                    ? (sizeKiB / 1024) + " MiB" : sizeKiB + " KiB");

            for (int strideIndex = 0; strideIndex < STRIDE_BYTES.length; strideIndex++) {
                int strideElements = STRIDE_BYTES[strideIndex] / Double.BYTES;
                times[sizeIndex][strideIndex] = measure(values, elementCount, strideElements);
                System.out.printf(Locale.ROOT, "%10.2f", times[sizeIndex][strideIndex]);
            }
            System.out.println();
        }

        estimateLineSize(times[times.length - 1]);
        estimateCacheSizes(times);
        printReportedCaches();
    }

    /** Runs a strided read loop and returns the median elapsed nanoseconds per access. */
    private static double measure(double[] values, int elementCount, int strideElements)
    {
        int accessesPerPass = (elementCount + strideElements - 1) / strideElements;
        long repetitions = Math.max(1, (TARGET_ACCESSES + accessesPerPass - 1) / accessesPerPass);

        // Warm up this exact access pattern before collecting timed samples.
        resultSink = readPass(values, elementCount, strideElements);
        double[] samples = new double[SAMPLES];

        for (int sample = 0; sample < SAMPLES; sample++) {
            double sum = 0.0;
            long start = System.nanoTime();
            for (long repetition = 0; repetition < repetitions; repetition++) {
                sum += readPass(values, elementCount, strideElements);
            }
            long elapsed = System.nanoTime() - start;
            resultSink = sum;
            samples[sample] = elapsed / (double) (repetitions * accessesPerPass);
        }

        // Sorting only the three sample times gives a simple outlier-resistant median.
        java.util.Arrays.sort(samples);
        return samples[SAMPLES / 2];
    }

    /** Reads every stride-th element in the selected prefix of the array. */
    private static double readPass(double[] values, int elementCount, int strideElements)
    {
        double sum = 0.0;
        for (int index = 0; index < elementCount; index += strideElements) {
            sum += values[index];
        }
        return sum;
    }

    /** Finds where access cost stops growing roughly with each doubling of stride. */
    private static void estimateLineSize(double[] largeWorkingSetTimes)
    {
        int consecutiveGrowthSteps = 0;
        int estimate = -1;
        // Below a cache line, larger strides can waste more of each fetched line and
        // raise time per access. Once accesses reach separate lines, that growth should
        // slow. Require two growth steps first so small-stride timing noise is ignored.
        for (int i = 1; i < STRIDE_BYTES.length; i++) {
            double ratio = largeWorkingSetTimes[i] / largeWorkingSetTimes[i - 1];
            if (ratio >= 1.5) {
                consecutiveGrowthSteps++;
            } else if (consecutiveGrowthSteps >= 2) {
                estimate = STRIDE_BYTES[i - 1];
                break;
            } else {
                consecutiveGrowthSteps = 0;
            }
        }

        if (estimate < 0) {
            System.out.println("Estimated cache line: no clear stride plateau in the tested range.");
        } else {
            System.out.println("Estimated cache-line scale: about " + estimate
                    + " B (time-per-access growth slowed after sustained increases; approximate, prefetch-sensitive).");
        }
    }

    /** Reports working-set intervals with a large latency rise at a 64-byte stride. */
    private static void estimateCacheSizes(double[][] times)
    {
        int referenceStride = 3; // 64-byte stride: one access per commonly-sized cache line.
        boolean found = false;
        System.out.println("Cache-size candidates from latency jumps at 64 B stride:");

        for (int i = 1; i < WORKING_SET_KIB.length; i++) {
            double previous = times[i - 1][referenceStride];
            double current = times[i][referenceStride];
            if (current >= previous * 1.5) {
                System.out.printf(Locale.ROOT, "  transition between %s and %s (%.2fx slower)%n",
                        formatSize(WORKING_SET_KIB[i - 1]), formatSize(WORKING_SET_KIB[i]),
                        current / previous);
                found = true;
            }
        }

        if (!found) {
            System.out.println("  no >= 1.5x latency jump found; the tested curve has no clear boundary.");
        }
        System.out.println("  These intervals are capacity clues, not exact cache-size measurements.");
    }

    /** Reads cache levels, capacities and line sizes reported by Linux under cpu0/cache. */
    private static void printReportedCaches()
    {
        Path cacheDirectory = sysFile.path("/sys/devices/system/cpu/cpu0/cache");
        List<Path> indices = new ArrayList<>();
        try (Stream<Path> entries = Files.list(cacheDirectory)) {
            entries.filter(path -> path.getFileName().toString().startsWith("index"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(indices::add);
        } catch (IOException e) {
            System.out.println("Linux sysfs cache data unavailable at " + cacheDirectory + ".");
            return;
        }

        if (indices.isEmpty()) {
            System.out.println("No cache index entries found under " + cacheDirectory + ".");
            return;
        }

        System.out.println("Machine-reported caches from " + cacheDirectory + ":");
        for (Path index : indices) {
            String level = readText(index.resolve("level"));
            String type = readText(index.resolve("type"));
            String size = readText(index.resolve("size"));
            String lineSize = readText(index.resolve("coherency_line_size"));
            System.out.printf("  L%s %-12s %s, line size %s B%n", unknown(level), unknown(type),
                    unknown(size), unknown(lineSize));
        }
    }

    /** Reads one sysfs text value and returns an empty string if that value is absent. */
    private static String readText(Path file)
    {
        try {
            return Files.readString(file).strip();
        } catch (IOException e) {
            return "";
        }
    }

    /** Makes missing sysfs fields visible in the comparison output. */
    private static String unknown(String value)
    {
        return value.isEmpty() ? "unknown" : value;
    }

    /** Formats a working-set size consistently in the estimate summary. */
    private static String formatSize(int kib)
    {
        return kib >= 1024 ? (kib / 1024) + " MiB" : kib + " KiB";//returns the size in MiB or KiB depending on the value of kib
    }
}
