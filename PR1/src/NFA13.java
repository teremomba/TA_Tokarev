// Количество состояний
private static final int TOTAL_STATES = 3;

// Количество символов алфавита
private static final int ALPHABET_CHARACTERS = 2;

private static final int UNKNOWN_SYMBOL_ERR = -1;
private static final int OK = 0;

// Состояния НКА
private enum NFA_STATES {
    q0, q1, q2
}

// Символы алфавита как индексы таблицы
private enum INPUT {
    ZERO, ONE
}

// Алфавит Σ = {0, 1}
private static final char[] ALPHABET = {'0', '1'};

/*
 * Таблица переходов НКА.
 *
 * TRANSITION_TABLE[from][symbol][to]
 *
 * true  - такой переход существует
 * false - такого перехода нет
 *
 * Благодаря третьему измерению одному состоянию
 * и одному символу в общем случае может
 * соответствовать несколько следующих состояний.
 */
private static final boolean[][][] TRANSITION_TABLE =
        new boolean
                [TOTAL_STATES]
                [ALPHABET_CHARACTERS]
                [TOTAL_STATES];

/*
 * Все состояния являются заключительными:
 * F = {q0, q1, q2}
 */

/*
 * Текущее множество состояний НКА.
 *
 * currentStates[i] == true означает,
 * что состояние qi сейчас активно.
 */
private static final boolean[] currentStates =
        new boolean[TOTAL_STATES];

/*
 * Множество состояний после обработки
 * очередного символа.
 */
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
     * q2 по символу 1 перехода не имеет:
     *
     * Δ(q2, 1) = ∅
     *
     * Именно последовательность:
     *
     * 1 -> 0 -> 1
     *
     * образует запрещённую подцепочку 101.
     */
}


// Поиск позиции символа в алфавите
private static int getSymbolPosition(char symbol) {

    for (int i = 0; i < ALPHABET_CHARACTERS; i++) {

        if (symbol == ALPHABET[i]) {
            return i;
        }
    }

    return UNKNOWN_SYMBOL_ERR;
}


// Возвращение автомата в начальное состояние
private static void reset() {

    Arrays.fill(currentStates, false);

    // Начальное множество состояний = {q0}
    currentStates[NFA_STATES.q0.ordinal()] = true;
}


// Обработка одного символа
private static int processSymbol(char symbol) {

    int position = getSymbolPosition(symbol);

    if (position == UNKNOWN_SYMBOL_ERR) {
        return UNKNOWN_SYMBOL_ERR;
    }

    /*
     * Перед очередным шагом очищаем
     * множество следующих состояний.
     */
    Arrays.fill(nextStates, false);

    /*
     * Проверяем переходы из каждого
     * активного состояния.
     */
    for (int from = 0; from < TOTAL_STATES; from++) {

        if (!currentStates[from]) {
            continue;
        }

        for (int to = 0; to < TOTAL_STATES; to++) {

            if (TRANSITION_TABLE[from][position][to]) {
                nextStates[to] = true;
            }
        }
    }

    /*
     * Полученное множество состояний
     * становится текущим.
     */
    System.arraycopy(nextStates, 0, currentStates, 0, TOTAL_STATES);

    return OK;
}


// Проверка допуска цепочки
private static boolean isAccepted() {

    /*
     * НКА принимает цепочку, если после
     * обработки всего входа активно хотя бы
     * одно заключительное состояние.
     */
    for (int i = 0; i < TOTAL_STATES; i++) {

        if (currentStates[i]) {
            return true;
        }
    }

    return false;
}


void main()
        throws IOException {

    setTransitions();
    reset();

    IO.println(
            "Enter a chain over alphabet {0, 1}:"
    );

    boolean error = false;
    int code;

    /*
     * Ввод по одному символу.
     * String не используется.
     */
    while ((code = System.in.read()) != -1) {

        char symbol = (char) code;

        // Windows: Enter передает \r\n
        if (symbol == '\r') {
            continue;
        }

        // Конец цепочки
        if (symbol == '\n') {
            break;
        }

        if (processSymbol(symbol)
                == UNKNOWN_SYMBOL_ERR) {

            error = true;
            break;
        }
    }

    if (!error && isAccepted()) {
        IO.println("Accepted");
    } else {
        IO.println("Rejected");
    }
}