import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/** Программная реализация МПА варианта 2 практической работы № 3. */
public final class Variant2PDA {
    private Variant2PDA() {
    }

    /** Один элемент отношения переходов δ(q, read, pop) ⊇ {(to, push)}. */
    private static final class Transition {
        final int from;
        final Character read; // null обозначает ε-переход
        final char pop;
        final int to;
        final String push;    // левый символ строки становится вершиной магазина

        Transition(int from, Character read, char pop, int to, String push) {
            this.from = from;
            this.read = read;
            this.pop = pop;
            this.to = to;
            this.push = push;
        }
    }

    /** Конфигурация (состояние, позиция во входе, содержимое магазина). */
    private static final class Configuration {
        final int state;
        final int position;
        final String stack;
        final Configuration previous;

        Configuration(int state, int position, String stack, Configuration previous) {
            this.state = state;
            this.position = position;
            this.stack = stack;
            this.previous = previous;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Configuration)) {
                return false;
            }
            Configuration other = (Configuration) object;
            return state == other.state
                    && position == other.position
                    && stack.equals(other.stack);
        }

        @Override
        public int hashCode() {
            int hash = 31 * state + position;
            return 31 * hash + stack.hashCode();
        }
    }

    private static final class Result {
        final boolean accepted;
        final Configuration last;

        Result(boolean accepted, Configuration last) {
            this.accepted = accepted;
            this.last = last;
        }
    }

    /** Семёрка P = (Q, Σ, Γ, δ, q0, Z0, F). */
    private static final class PDA {
        final Set<Integer> states;
        final Set<Character> inputAlphabet;
        final Set<Character> stackAlphabet;
        final List<Transition> transitions;
        final int initialState;
        final char initialStackSymbol;
        final Set<Integer> finalStates;

        PDA(Set<Integer> states, Set<Character> inputAlphabet,
            Set<Character> stackAlphabet, List<Transition> transitions,
            int initialState, char initialStackSymbol, Set<Integer> finalStates) {
            if (!states.contains(initialState)
                    || !stackAlphabet.contains(initialStackSymbol)
                    || !states.containsAll(finalStates)) {
                throw new IllegalArgumentException("Некорректные компоненты МПА");
            }
            for (Transition transition : transitions) {
                if (!states.contains(transition.from)
                        || !states.contains(transition.to)
                        || !stackAlphabet.contains(transition.pop)
                        || (transition.read != null
                            && !inputAlphabet.contains(transition.read))) {
                    throw new IllegalArgumentException("Некорректный переход МПА");
                }
                for (int i = 0; i < transition.push.length(); i++) {
                    if (!stackAlphabet.contains(transition.push.charAt(i))) {
                        throw new IllegalArgumentException("Некорректный символ магазина");
                    }
                }
            }
            this.states = states;
            this.inputAlphabet = inputAlphabet;
            this.stackAlphabet = stackAlphabet;
            this.transitions = transitions;
            this.initialState = initialState;
            this.initialStackSymbol = initialStackSymbol;
            this.finalStates = finalStates;
        }

        Result recognize(String input) {
            Configuration initial = new Configuration(
                    initialState, 0, String.valueOf(initialStackSymbol), null);
            Queue<Configuration> queue = new ArrayDeque<Configuration>();
            Set<Configuration> visited = new HashSet<Configuration>();
            queue.add(initial);
            visited.add(initial);
            Configuration furthest = initial;

            while (!queue.isEmpty()) {
                Configuration current = queue.remove();
                if (current.position > furthest.position) {
                    furthest = current;
                }

                // Критерий: весь вход прочитан и достигнуто заключительное состояние.
                if (current.position == input.length()
                        && finalStates.contains(current.state)) {
                    return new Result(true, current);
                }

                for (Transition transition : transitions) {
                    if (transition.from != current.state || current.stack.isEmpty()
                            || current.stack.charAt(0) != transition.pop) {
                        continue;
                    }

                    int nextPosition = current.position;
                    if (transition.read != null) {
                        if (nextPosition >= input.length()
                                || !inputAlphabet.contains(input.charAt(nextPosition))
                                || input.charAt(nextPosition) != transition.read.charValue()) {
                            continue;
                        }
                        nextPosition++;
                    }

                    // Вершина магазина записана слева: снять pop, затем поместить push.
                    String nextStack = transition.push + current.stack.substring(1);
                    Configuration next = new Configuration(
                            transition.to, nextPosition, nextStack, current);
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
            return new Result(false, furthest);
        }
    }

    private static PDA buildVariant2() {
        // Q={q0}, Σ={a,b}, Γ={Z}, q0=0, Z0=Z, F=∅.
        Set<Integer> states = Collections.singleton(0);
        Set<Character> inputAlphabet = new HashSet<Character>(Arrays.asList('a', 'b'));
        Set<Character> stackAlphabet = Collections.singleton('Z');
        List<Transition> delta = Arrays.asList(
                new Transition(0, 'a', 'Z', 0, "Z"),
                new Transition(0, 'b', 'Z', 0, "Z"));
        Set<Integer> finalStates = Collections.emptySet();
        return new PDA(states, inputAlphabet, stackAlphabet,
                delta, 0, 'Z', finalStates);
    }

    private static void printResult(PDA pda, String input) {
        Result result = pda.recognize(input);
        String shownInput = input.isEmpty() ? "ε" : input;
        System.out.println("Цепочка: " + shownInput);

        List<Configuration> path = new ArrayList<Configuration>();
        for (Configuration step = result.last; step != null; step = step.previous) {
            path.add(step);
        }
        Collections.reverse(path);
        for (Configuration step : path) {
            String unread = input.substring(step.position);
            System.out.println("  (q" + step.state + ", "
                    + (unread.isEmpty() ? "ε" : unread) + ", "
                    + (step.stack.isEmpty() ? "ε" : step.stack) + ")");
        }
        System.out.println("Результат: " + (result.accepted ? "Accept" : "Reject"));
    }

    public static void main(String[] args) throws IOException {
        PDA pda = buildVariant2();
        System.out.println("Критерий принятия: по заключительному состоянию.");

        if (args.length != 0) {
            for (String arg : args) {
                printResult(pda, "eps".equals(arg) || "ε".equals(arg) ? "" : arg);
            }
            return;
        }

        System.out.println("Введите цепочки над {a,b} по одной в строке; пустая строка — ε."
                + " Для завершения введите exit.");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            if ("exit".equalsIgnoreCase(line.trim())) {
                break;
            }
            printResult(pda, line);
        }
    }
}
