package pl.poznan.put.cs.bioserver.comparison;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.biojava.bio.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstraction of all global comparison measures.
 * 
 * @author Tomasz Żok (tzok[at]cs.put.poznan.pl)
 */
public abstract class GlobalComparison {
    private class CompareCallable implements Callable<SingleResult> {
        private Structure s1;
        private Structure s2;
        private int i;
        private int j;

        public CompareCallable(Structure[] structures, int i, int j) {
            s1 = structures[i];
            s2 = structures[j];
            this.i = i;
            this.j = j;
        }

        @Override
        public SingleResult call() throws Exception {
            SingleResult result = new SingleResult();
            result.i = i;
            result.j = j;
            result.value = compare(s1, s2);
            return result;
        }
    }

    private static class SingleResult {
        private int i;
        private int j;
        private double value;
    }

    private static final Logger LOGGER = LoggerFactory
            .getLogger(GlobalComparison.class);

    /**
     * Compare two structures.
     * 
     * @param s1
     *            First structure.
     * @param s2
     *            Second structure.
     * @return Distance between the structures according to some measure.
     * @throws IncomparableStructuresException
     *             If the two structure could not be compared.
     */
    public abstract double compare(Structure s1, Structure s2)
            throws IncomparableStructuresException;

    /**
     * Compare each structures with each other.
     * 
     * @param structures
     *            An array of structures to be compared.
     * @return A distance matrix.
     */
    public double[][] compare(final Structure[] structures,
            final ComparisonListener listener) {
        /*
         * Create distance matrix, set diagonal to 0 and other values to NaN
         */
        double[][] matrix = new double[structures.length][];
        for (int i = 0; i < structures.length; ++i) {
            matrix[i] = new double[structures.length];
        }
        for (int i = 0; i < structures.length; i++) {
            for (int j = 0; j < structures.length; j++) {
                if (i == j) {
                    matrix[i][j] = 0;
                } else {
                    matrix[i][j] = Double.NaN;
                }
            }
        }

        /*
         * Create a fixed pool of threads and a service to gather results from
         * each calculation
         */
        int countThreads = Runtime.getRuntime().availableProcessors() * 2;
        final ExecutorService threadPool = Executors
                .newFixedThreadPool(countThreads);
        ExecutorCompletionService<SingleResult> ecs = new ExecutorCompletionService<>(
                threadPool);
        for (int i = 0; i < structures.length; i++) {
            for (int j = i + 1; j < structures.length; j++) {
                CompareCallable task = new CompareCallable(structures, i, j);
                ecs.submit(task);
            }
        }
        threadPool.shutdown();

        /*
         * In a separate thread, inform a listener about current status of
         * execution
         */
        final long all = structures.length * (structures.length - 1) / 2;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
                        if (listener != null) {
                            listener.stateChanged(all,
                                    ((ThreadPoolExecutor) threadPool)
                                            .getCompletedTaskCount());
                        }
                    }
                    if (listener != null) {
                        listener.stateChanged(all,
                                ((ThreadPoolExecutor) threadPool)
                                        .getCompletedTaskCount());
                    }
                } catch (InterruptedException e) {
                    threadPool.shutdownNow();
                }
            }
        });
        thread.start();

        /*
         * Finally gather the results back in the matrix
         */
        for (int i = 0; i < all; i++) {
            try {
                SingleResult result = ecs.take().get();
                matrix[result.i][result.j] = result.value;
                matrix[result.j][result.i] = result.value;
            } catch (InterruptedException | ExecutionException e) {
                GlobalComparison.LOGGER.error(
                        "Failed to compare a pair of structures", e);
            }
        }
        return matrix;
    }
}
