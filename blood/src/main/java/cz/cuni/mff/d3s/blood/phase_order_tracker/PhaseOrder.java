package cz.cuni.mff.d3s.blood.phase_order_tracker;

public class PhaseOrder {
    private static ThreadLocal<Integer> phaseNumber = ThreadLocal.withInitial(() -> 0);

    public static int getCurrentPhaseNumber() {
        return phaseNumber.get();
    }

    public static void resetCounter() {
        phaseNumber.set(0);
        System.out.println("resetting, threadID=" + Thread.currentThread().getId() + ", value="+phaseNumber.get());
    }

    public static void incrementCounter() {
        phaseNumber.set(phaseNumber.get() + 1);
        System.out.println("increment, threadID=" + Thread.currentThread().getId() + ", value="+phaseNumber.get());
    }
}
