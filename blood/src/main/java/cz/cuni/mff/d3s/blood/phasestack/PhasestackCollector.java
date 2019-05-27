package cz.cuni.mff.d3s.blood.phasestack;

import cz.cuni.mff.d3s.blood.report.TextDump;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import java.util.stream.Collectors;

public class PhasestackCollector extends TextDump {

    private final Deque<Class> stack = new ArrayDeque<>();
    private final List<String> states = new LinkedList<>();

    /**
     * stackStateID will always correspond to the index to
     * {@link PhasestackCollector#states}, under which the representation of current
     * state of {@link PhasestackCollector#stack} will once be accessible.
     */
    public int stackStateID = -1;

    public void push(Class<?> phaseClass) {
        stackStateID++;

        stack.addLast(phaseClass);

        // update dump
        states.add(currentStateToString());
    }

    public void pop(Class<?> phaseClass) {
        stackStateID++;

        var out = stack.pollLast();

        // sanity check
        if (out != phaseClass) {
            throw new AssertionError("Exiting phase that was not supposed to exit right now!");
        }

        // update dump
        states.add(currentStateToString());
    }

    public String currentStateToString() {
        return stack.stream().map(aClass -> aClass.getName()).collect(Collectors.joining(" "));
    }

    @Override
    public String getName() {
        return "phasestack";
    }

    @Override
    public String getText() {
        return String.join("\n", states);
    }
}
