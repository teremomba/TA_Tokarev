import java.io.IOException;

public class DFA13 {

    private static final int TOTAL_STATES = 3;
    private static final int ALPHABET_CHARACTERS = 2;

    private static final int UNKNOWN_SYMBOL_ERR = -1;
    private static final int OK = 0;

    private enum DFA_STATES {
        q0, q1, q2
    }

    private enum INPUT {
        A, B
    }

    // Алфавит Σ = {a, b}
    private static final char[] ALPHABET = {'a', 'b'};

    // Таблица переходов
    private static final int[][] TRANSITION_TABLE =
            new int[TOTAL_STATES][ALPHABET_CHARACTERS];

    // F = {q2}
    private static final boolean[] FINAL_STATES = {
            false,
            false,
            true
    };

    // Начальное состояние
    private static int currentState =
            DFA_STATES.q0.ordinal();


    // Заполнение таблицы переходов
    private static void setTransitions() {

        // q0 --a--> q1
        TRANSITION_TABLE
                [DFA_STATES.q0.ordinal()]
                [INPUT.A.ordinal()] =
                DFA_STATES.q1.ordinal();

        // q0 --b--> q0
        TRANSITION_TABLE
                [DFA_STATES.q0.ordinal()]
                [INPUT.B.ordinal()] =
                DFA_STATES.q0.ordinal();

        // q1 --a--> q1
        TRANSITION_TABLE
                [DFA_STATES.q1.ordinal()]
                [INPUT.A.ordinal()] =
                DFA_STATES.q1.ordinal();

        // q1 --b--> q2
        TRANSITION_TABLE
                [DFA_STATES.q1.ordinal()]
                [INPUT.B.ordinal()] =
                DFA_STATES.q2.ordinal();

        // q2 --a--> q2
        TRANSITION_TABLE
                [DFA_STATES.q2.ordinal()]
                [INPUT.A.ordinal()] =
                DFA_STATES.q2.ordinal();

        // q2 --b--> q2
        TRANSITION_TABLE
                [DFA_STATES.q2.ordinal()]
                [INPUT.B.ordinal()] =
                DFA_STATES.q2.ordinal();
    }


    // Определение позиции символа в алфавите
    private static int getSymbolPosition(char symbol) {

        for (int i = 0; i < ALPHABET_CHARACTERS; i++) {

            if (symbol == ALPHABET[i]) {
                return i;
            }
        }

        return UNKNOWN_SYMBOL_ERR;
    }


    // Вывод состояния
    private static void printState(int state) {

        System.out.print("q");
        System.out.print(state);
    }


    // Один шаг работы ДКА
    private static int processSymbol(char symbol, int step) {

        int position = getSymbolPosition(symbol);

        if (position == UNKNOWN_SYMBOL_ERR) {
            return UNKNOWN_SYMBOL_ERR;
        }

        // Запоминаем состояние до перехода
        int previousState = currentState;

        // Переход по таблице
        currentState =
                TRANSITION_TABLE[currentState][position];

        // Вывод шага
        System.out.print("Step ");
        System.out.print(step);
        System.out.print(": ");

        printState(previousState);

        System.out.print(" --");
        System.out.print(symbol);
        System.out.print("--> ");

        printState(currentState);

        System.out.println();

        return OK;
    }


    // Проверка допуска
    private static boolean isAccepted() {

        return FINAL_STATES[currentState];
    }


    // Сброс автомата
    private static void reset() {

        currentState = DFA_STATES.q0.ordinal();
    }


    public static void main(String[] args)
            throws IOException {

        setTransitions();
        reset();

        System.out.println(
                "Enter a chain over alphabet {a, b}:"
        );

        boolean error = false;
        int code;
        int step = 0;

        System.out.println();

        // Начальное состояние
        System.out.print("Step 0: ");
        printState(currentState);
        System.out.println();

        while ((code = System.in.read()) != -1) {

            char symbol = (char) code;

            if (symbol == '\r') {
                continue;
            }

            if (symbol == '\n') {
                break;
            }

            step++;

            if (processSymbol(symbol, step)
                    == UNKNOWN_SYMBOL_ERR) {

                System.out.println(
                        "Unknown symbol: " + symbol
                );

                error = true;
                break;
            }
        }

        System.out.println();

        if (!error && isAccepted()) {
            System.out.println("Result: Accepted");
        } else {
            System.out.println("Result: Rejected");
        }
    }
}