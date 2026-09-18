import java.io.IOException;

public class NFA13 {

    private static final int TOTAL_STATES = 3;
    private static final int ALPHABET_CHARACTERS = 2;

    private static final int UNKNOWN_SYMBOL_ERR = -1;
    private static final int OK = 0;

    private enum NFA_STATES {
        q0, q1, q2
    }

    private enum INPUT {
        ZERO, ONE
    }

    // Алфавит Σ = {0, 1}
    private static final char[] ALPHABET = {'0', '1'};

    /*
     * Таблица переходов НКА:
     *
     * TRANSITION_TABLE[from][symbol][to]
     */
    private static final boolean[][][] TRANSITION_TABLE =
            new boolean
                    [TOTAL_STATES]
                    [ALPHABET_CHARACTERS]
                    [TOTAL_STATES];

    // Все три состояния допускающие
    private static final boolean[] FINAL_STATES = {
            true,
            true,
            true
    };

    // Текущее множество состояний
    private static final boolean[] currentStates =
            new boolean[TOTAL_STATES];

    // Следующее множество состояний
    private static final boolean[] nextStates =
            new boolean[TOTAL_STATES];


    // Заполнение таблицы переходов
    private static void setTransitions() {

        // q0 --0--> q0
        TRANSITION_TABLE
                [NFA_STATES.q0.ordinal()]
                [INPUT.ZERO.ordinal()]
                [NFA_STATES.q0.ordinal()] = true;

        // q0 --1--> q1
        TRANSITION_TABLE
                [NFA_STATES.q0.ordinal()]
                [INPUT.ONE.ordinal()]
                [NFA_STATES.q1.ordinal()] = true;

        // q1 --0--> q2
        TRANSITION_TABLE
                [NFA_STATES.q1.ordinal()]
                [INPUT.ZERO.ordinal()]
                [NFA_STATES.q2.ordinal()] = true;

        // q1 --1--> q1
        TRANSITION_TABLE
                [NFA_STATES.q1.ordinal()]
                [INPUT.ONE.ordinal()]
                [NFA_STATES.q1.ordinal()] = true;

        // q2 --0--> q0
        TRANSITION_TABLE
                [NFA_STATES.q2.ordinal()]
                [INPUT.ZERO.ordinal()]
                [NFA_STATES.q0.ordinal()] = true;

        /*
         * q2 --1--> ∅
         *
         * Переход отсутствует.
         */
    }


    // Поиск символа в алфавите
    private static int getSymbolPosition(char symbol) {

        for (int i = 0; i < ALPHABET_CHARACTERS; i++) {

            if (symbol == ALPHABET[i]) {
                return i;
            }
        }

        return UNKNOWN_SYMBOL_ERR;
    }


    // Сброс автомата
    private static void reset() {

        for (int i = 0; i < TOTAL_STATES; i++) {
            currentStates[i] = false;
        }

        // Начальное множество = {q0}
        currentStates[NFA_STATES.q0.ordinal()] = true;
    }


    // Вывод множества состояний
    private static void printStates(boolean[] states) {

        System.out.print("{");

        boolean first = true;

        for (int i = 0; i < TOTAL_STATES; i++) {

            if (states[i]) {

                if (!first) {
                    System.out.print(", ");
                }

                System.out.print("q");
                System.out.print(i);

                first = false;
            }
        }

        System.out.print("}");
    }


    // Один шаг работы НКА
    private static int processSymbol(char symbol, int step) {

        int position =
                getSymbolPosition(symbol);

        if (position == UNKNOWN_SYMBOL_ERR) {
            return UNKNOWN_SYMBOL_ERR;
        }

        /*
         * Сначала показываем состояние
         * до обработки символа.
         */
        System.out.print("Step ");
        System.out.print(step);
        System.out.print(": ");

        printStates(currentStates);

        System.out.print(" --");
        System.out.print(symbol);
        System.out.print("--> ");

        // Очищаем следующее множество
        for (int i = 0; i < TOTAL_STATES; i++) {
            nextStates[i] = false;
        }

        /*
         * Для каждого активного состояния
         * находим все возможные переходы.
         */
        for (int from = 0;
             from < TOTAL_STATES;
             from++) {

            if (!currentStates[from]) {
                continue;
            }

            for (int to = 0;
                 to < TOTAL_STATES;
                 to++) {

                if (TRANSITION_TABLE
                        [from]
                        [position]
                        [to]) {

                    nextStates[to] = true;
                }
            }
        }

        // Показываем результат перехода
        printStates(nextStates);

        System.out.println();

        // Следующее множество становится текущим
        for (int i = 0; i < TOTAL_STATES; i++) {
            currentStates[i] = nextStates[i];
        }

        return OK;
    }


    // Проверка допуска
    private static boolean isAccepted() {

        for (int i = 0; i < TOTAL_STATES; i++) {

            if (currentStates[i]
                    && FINAL_STATES[i]) {

                return true;
            }
        }

        return false;
    }


    public static void main(String[] args)
            throws IOException {

        setTransitions();
        reset();

        System.out.println(
                "Enter a chain over alphabet {0, 1}:"
        );

        boolean error = false;
        int code;
        int step = 0;

        System.out.println();

        // Начальное множество состояний
        System.out.print("Step 0: ");
        printStates(currentStates);
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