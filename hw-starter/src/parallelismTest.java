import java.util.ArrayList;
import java.util.List;

// parallelismTest class tests how execution time changes when a cpu-heavy task
// is divided among different numbers of java threads
public class parallelismTest {

    private static final int WORK_PER_THREAD = 1_000_000_000; // Number of calculations each thread will perform.
    private static volatile double resultSink; // stores the result of a worker's calculation. volatile makes the writes
                                               // visible across threads

    private static long[][] readCpuStats() throws Exception {
        List<long[]> cpuStats = new ArrayList<>();

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader("/proc/stat"))) {
            String line;

            while ((line = reader.readLine()) != null) { // Linux reports aggregate CPU info first, then individual CPUs
                if (!line.matches("^cpu\\d+\\s+.*")) {
                    if (line.startsWith("cpu")) {
                        continue;
                    }
                    if (!cpuStats.isEmpty()) { // CPU entries appear at the start of /proc/stat
                        break; // Stops once the entries have ended
                    }
                    continue;
                }
                String[] fields = line.trim().split("\\s+");
                long idle = Long.parseLong(fields[4]);

                // Include I/O wait in the idle time for this calculation.
                if (fields.length > 5) {
                    idle += Long.parseLong(fields[5]);
                }
                long total = 0;

                // Add the CPU time counters together.
                for (int i = 1; i < fields.length; i++) {
                    total += Long.parseLong(fields[i]);
                }
                cpuStats.add(new long[] { total, idle });
            }
        }
        return cpuStats.toArray(new long[0][]);
    }

    // Calculate the percentage of time each logical CPU was busy
    // between two samples of /proc/stat.
    private static void printCpuLoads(long[][] previous, long[][] current) {
        System.out.println("Per-logical-CPU load during workload:");

        int cpuCount = Math.min(previous.length, current.length);

        for (int i = 0; i < cpuCount; i++) {
            long totalDifference = current[i][0] - previous[i][0];
            long idleDifference = current[i][1] - previous[i][1];

            double usage = 0.0;

            if (totalDifference > 0) {
                usage = 100.0 * (totalDifference - idleDifference) / totalDifference;
            }

            System.out.printf("  CPU%-2d: %.1f%%%n", i, usage);
        }
    }

    // Continuously monitor CPU usage while the workload is running.
    private static void monitorCpuLoad(java.util.concurrent.atomic.AtomicBoolean running,
            int threadCount) {

        try {
            // Take an initial CPU statistics sample.
            long[][] previous = readCpuStats();

            while (running.get()) {
                // Wait 100 milliseconds between samples.
                Thread.sleep(100);
                long[][] current = readCpuStats();

                // Display CPU activity during the workload.
                if (running.get()) {
                    System.out.println(
                            "\nCPU load during " + threadCount + "-thread test:");
                    printCpuLoads(previous, current);
                }
                previous = current;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("CPU monitoring error: " + e.getMessage());
        }
    }

    // Perform CPU-intensive arithmetic by taking the square root of a large
    // Each thread performs the same amount of work, so the
    // workload grows when the number of threads increases.

    private static void doWork(int work) { // work is the number of iterations each thread performs
        double result = 0.0;
        for (int i = 1; i <= work; i++) {
            result += Math.sqrt(i); // this is the cpu-intensive task that is being timed and split up across
        }
        resultSink = result; // Store the calculated result so it has an observable effect.
    }

    private static double runTest(int threadCount, boolean enableMonitoring) throws InterruptedException {

        List<Thread> threads = new ArrayList<>();
        int workPerWorker = WORK_PER_THREAD / threadCount;

        // Control when the monitoring thread starts and stops.
        java.util.concurrent.atomic.AtomicBoolean monitoring = new java.util.concurrent.atomic.AtomicBoolean(
                enableMonitoring);
        Thread monitor = null;
        if (enableMonitoring) {
            monitor = new Thread(() -> monitorCpuLoad(monitoring, threadCount)); // Create the CPU monitoring thread.
        }
        long startTime = System.nanoTime(); // Start timing immediately before starting the workers.
        if (monitor != null) {
            monitor.start(); // Start monitoring while the workload runs.
        }

        // Start the worker threads.
        for (int i = 0; i < threadCount; i++) {
            Thread worker = new Thread(() -> doWork(workPerWorker));
            threads.add(worker);
            worker.start();
        }

        // Wait until all worker threads finish.
        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.nanoTime();

        // Stop monitoring after the workload finishes.
        monitoring.set(false);

        if (monitor != null) {
            monitor.interrupt(); // Wait for the monitoring thread to finish.
            monitor.join();
        }
        // Return the workload execution time in seconds.
        return (endTime - startTime) / 1_000_000_000.0;
    }

    // Calculates the theoretical speedup using Amdahl's law.
    private static double amdahlSpeedup(int threadCount, double parallelFraction) { // Parallel fraction is the fravtion
                                                                                    // of the workload that can run in
                                                                                    // parallel
        double serialFraction = 1.0 - parallelFraction; // Calculates the fraction of work that must remain serial
        return 1.0 / (serialFraction + parallelFraction / threadCount); // Apply Amdahl's Law to estimate theoretical
                                                                        // speedup
    }

    public static void main(String[] args) throws InterruptedException {

        int[] threadCounts = { 1, 2, 4, 8 }; // array of threads we want to compare
        System.out.println("Warming up the JVM"); // JIT can optimise the code if it is ran multiple times

        for (int threadCount : threadCounts) {
            runTest(threadCount, false);
        }

        System.out.println("Parallelism Experiment");
        System.out.println("======================");

        double baselineTime = 0.0; // this stores the timei taken by a thread to complete the task

        for (int threadCount : threadCounts) { // executes the workload and measures its duration
            double executionTime = runTest(threadCount, true);

            if (threadCount == 1) {
                baselineTime = executionTime;
            }
            double speedup = baselineTime / executionTime; // calculates the speedup relative to the one-thread run
            double theoreticalSpeedup = amdahlSpeedup(threadCount, 0.99); // Assuming that 99% of the workload can run
                                                                          // in parallel

            // Display measured performance beside the theoretical prediction.
            System.out.printf(
                    "%d thread(s): %.3f seconds | Measured: %.2fx | Amdahl: %.2fx%n",
                    threadCount,
                    executionTime,
                    speedup,
                    theoreticalSpeedup);
        }
    }
}