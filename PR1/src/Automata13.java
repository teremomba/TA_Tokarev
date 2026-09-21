import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Table-driven automata. No string-processing methods are used. */
public final class Automata13 {
    private Automata13() { }

    public interface Machine {
        void reset();
        void step(int symbol);
        boolean accepted();
        boolean validAlphabet();
        void printStates(PrintStream out);
    }

    private static int column(int symbol, char[] alphabet) {
        for (int i = 0; i < alphabet.length; i++) {
            if (symbol == alphabet[i]) return i;
        }
        return -1;
    }

    public static final class DFA implements Machine {
        private static final char[] ALPHABET = {'a', 'b'};
        private static final int[][] TRANSITIONS = {
            {1, 0}, // q0
            {1, 2}, // q1
            {2, 2}  // q2
        };
        private static final boolean[] FINAL = {false, false, true};
        private int state;
        private boolean valid;

        public DFA() { reset(); }
        public void reset() { state = 0; valid = true; }
        public void step(int symbol) {
            int col = column(symbol, ALPHABET);
            if (col < 0) { valid = false; return; }
            if (valid) state = TRANSITIONS[state][col];
        }
        public boolean accepted() { return valid && FINAL[state]; }
        public boolean validAlphabet() { return valid; }
        public void printStates(PrintStream out) {
            if (!valid) { out.print("{}"); return; }
            out.print('q'); out.print(state);
        }
    }

    public static final class NFA implements Machine {
        private static final char[] ALPHABET = {'0', '1'};
        // Each cell is the complete set of destination state numbers.
        private static final int[][][] TRANSITIONS = {
            {{0}, {1, 3}}, // q0: real nondeterministic choice on 1
            {{2}, {1}},    // q1: continuation through a later zero
            {{0}, {}},     // q2: 101 is forbidden
            {{}, {3}}      // q3: hypothesis that only ones remain
        };
        private static final boolean[] FINAL = {true, false, true, true};
        private boolean[] current;
        private boolean[] next;
        private boolean valid;

        public NFA() { reset(); }
        public void reset() {
            current = new boolean[4];
            next = new boolean[4];
            current[0] = true;
            valid = true;
        }
        public void step(int symbol) {
            int col = column(symbol, ALPHABET);
            if (col < 0) { valid = false; return; }
            if (!valid) return;
            for (int q = 0; q < next.length; q++) next[q] = false;
            for (int q = 0; q < current.length; q++) {
                if (!current[q]) continue;
                int[] destinations = TRANSITIONS[q][col];
                for (int i = 0; i < destinations.length; i++) {
                    next[destinations[i]] = true;
                }
            }
            boolean[] old = current;
            current = next;
            next = old;
        }
        public boolean accepted() {
            if (!valid) return false;
            for (int q = 0; q < current.length; q++) {
                if (current[q] && FINAL[q]) return true;
            }
            return false;
        }
        public boolean validAlphabet() { return valid; }
        public void printStates(PrintStream out) {
            out.print('{');
            boolean first = true;
            if (valid) {
                for (int q = 0; q < current.length; q++) {
                    if (!current[q]) continue;
                    if (!first) out.print(',');
                    out.print('q'); out.print(q);
                    first = false;
                }
            }
            out.print('}');
        }
    }

    private static void finish(Machine machine, boolean hasSymbols, PrintStream out) {
        if (!hasSymbols) out.print("EPS");
        out.print('\t');
        out.print(machine.accepted() ? "Accept" : "Reject");
        out.print('\t');
        machine.printStates(out);
        if (!machine.validAlphabet()) out.print("\tOUTSIDE_ALPHABET");
        out.println();
    }

    /** One physical input line is one test; a blank line denotes epsilon. */
    public static void run(Machine machine, Reader input, PrintStream out) throws IOException {
        out.println("Input\tResult\tStates");
        machine.reset();
        boolean hasSymbols = false;
        boolean afterCR = false;
        int symbol;
        while ((symbol = input.read()) != -1) {
            if (afterCR && symbol == '\n') { afterCR = false; continue; }
            afterCR = false;
            if (symbol == '\r' || symbol == '\n') {
                finish(machine, hasSymbols, out);
                machine.reset();
                hasSymbols = false;
                afterCR = symbol == '\r';
            } else {
                out.print((char) symbol);
                machine.step(symbol);
                hasSymbols = true;
            }
        }
        if (hasSymbols) finish(machine, true, out);
    }

    public static void launch(Machine machine, String[] args) throws IOException {
        if (args.length > 1) {
            System.err.println("Use zero arguments (stdin) or one UTF-8 input file.");
            System.exit(2);
        }
        // String[] is required by Java; its elements are only file paths.
        Reader input = new BufferedReader(new InputStreamReader(
            args.length == 0 ? System.in : new FileInputStream(args[0]),
            StandardCharsets.UTF_8));
        run(machine, input, System.out);
        if (args.length != 0) input.close();
    }
}
