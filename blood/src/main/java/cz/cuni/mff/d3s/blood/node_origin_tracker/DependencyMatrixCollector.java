package cz.cuni.mff.d3s.blood.node_origin_tracker;

import cz.cuni.mff.d3s.blood.method_local.CompilationEventLocal;
import cz.cuni.mff.d3s.blood.report.Report;
import cz.cuni.mff.d3s.blood.report.dump.ManualTextDump;
import cz.cuni.mff.d3s.blood.utils.ConcurrentMatrix;
import cz.cuni.mff.d3s.blood.utils.ConcurrentOrderedSet;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public final class DependencyMatrixCollector {
    // TODO make this configurable, also works with `new DefaultNodeTracker()`
    private static NodeTracker nodeTracker = new CustomNodeTracker();

    private static CompilationEventLocal<DependencyMatrix> matrix = new CompilationEventLocal<>(DependencyMatrix::new, dependencyMatrix -> Report.getInstance().dumpNow(new ManualTextDump("depmat", dependencyMatrix::dump)));

    /**
     * This function is called by the instrumentation after the Node class is
     * initialized.
     */
    public static void onNodeClassInit() {
        // initialize everything needed by the node tracker implementation
        nodeTracker.onNodeClassInit();
    }

    /**
     * This function is called by the instrumentation before every optimization
     * phase run. More specifically, before calling
     * {@link org.graalvm.compiler.phases.BasePhase#apply(StructuredGraph, Object)}
     *
     * @param graph       Graph entering the optimization phase
     * @param sourceClass Class of the optimization phase running
     */
    public static void prePhase(StructuredGraph graph, Class<?> sourceClass) {
        matrix.get().update(graph, sourceClass);
    }

    /**
     * This function is called by the instrumentation after every optimization
     * phase run. More specifically, after calling
     * {@link org.graalvm.compiler.phases.BasePhase#apply(StructuredGraph, Object)}
     *
     * @param graph       Graph representing IL after being processed by the
     *                    optimization phase
     * @param sourceClass Class of the running optimization phase
     */
    public static void postPhase(StructuredGraph graph, Class<?> sourceClass) {
        nodeTracker.updateCreationPhase(graph.getNodes(), sourceClass);
    }

    static final class DependencyMatrix {
        // the default of 16 doesn't fit even the most trivial programs
        private static final int HASHMAP_INIT_CAPACITY = 64;

        /**
         * Multiple threads are writing to the result matrix at once. That's fine.
         * However, in the end, we want to dump the data and nobody should be
         * writing at that time. We can achieve this kind of locking by using
         * {@link ReadWriteLock} in the opposite way, than it was designed. Lock for
         * reading, when we are writing. Lock for writing, when we are reading.
         */
        private final ReadWriteLock writers = new ReentrantReadWriteLock(true);


        private final ConcurrentOrderedSet<Class> phaseOrder = new ConcurrentOrderedSet<>();
        private final ConcurrentMatrix<Class, Class, DependencyValue> matrix = new ConcurrentMatrix<>(HASHMAP_INIT_CAPACITY, DependencyValue.ZERO);

        public DependencyMatrix() {
            phaseOrder.add(NodeTracker.NoPhaseDummy.class);
            phaseOrder.add(NodeTracker.DeletedPhaseDummy.class);
        }

        public String dump() {
            // Block, so that nobody can write to the matrix.
            Lock writeLock = writers.writeLock();
            writeLock.lock();
            try {
                String header = phaseOrder.stream()
                        .map(Class::getName)
                        .collect(Collectors.joining("\n"));

                String data = matrix.toString(phaseOrder::stream, phaseOrder::stream);

                return header + "\n\n" + data;
            } finally {
                writeLock.unlock();
            }

        }

        public void update(StructuredGraph graph, Class sourceClass) {
            Lock readLock = writers.readLock();
            if (!readLock.tryLock()) {
                // Dump is already in progress, don't change the matrix any more.
                return;
            }

            try {
                phaseOrder.add(sourceClass);

                // obtain row in result matrix for this particular optimization phase
                var row = matrix.getOrCreateRow(sourceClass);

                // for each node in the graph entering the phase, note down where it was created
                for (Node node : graph.getNodes()) {
                    var creationPhaseResult = nodeTracker.getCreationPhase(node);

                    if (creationPhaseResult.isError()) {
                        System.err.println(creationPhaseResult.unwrapError());
                        continue;
                    }

                    var creationPhaseClass = creationPhaseResult.unwrap();

                    DependencyValue value = row.getOrCreate(creationPhaseClass);
                    value.incrementNumberOfSeenNodes(1);
                }

                // update total node counts for all tracked values
                row.valuesStream().forEach(value -> {
                    value.incrementTotalNumberOfNodesSeen(graph.getNodeCount());
                    value.incrementPhaseCounter();
                });

            } finally {
                // don't forget to unlock the lock
                readLock.unlock();
            }
        }
    }
}
