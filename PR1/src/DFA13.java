// Количество состояний
private static final int TOTAL_STATES = 3;

// Количество символов алфавита
private static final int ALPHABET_CHARACTERS = 2;

// Коды результата обработки символа
private static final int UNKNOWN_SYMBOL_ERR = -1;
private static final int OK = 0;

// Состояния ДКА
private enum DFA_STATES {
    q0, q1, q2
}

// Символы алфавита как индексы таблицы
private enum INPUT {
    A, B
}

// Алфавит Σ = {a, b}
private static final char[] ALPHABET = {'a', 'b'};

/*
 * Таблица переходов.
 *
 * Строка     - текущее состояние.
 * Столбец    - входной символ.
 * Значение   - следующее состояние.
 */
private static final int[][] TRANSITION_TABLE =
        new int[TOTAL_STATES][ALPHABET_CHARACTERS];

// Множество заключительных состояний F = {q2}
private static final boolean[] FINAL_STATES = {
        false,
        false,
        true
};

// Начальное состояние q0
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


// Определяем номер символа в алфавите
private static int getSymbolPosition(char symbol) {

    for (int i = 0; i < ALPHABET_CHARACTERS; i++) {

        if (symbol == ALPHABET[i]) {
            return i;
        }
    }

    return UNKNOWN_SYMBOL_ERR;
}


// Один шаг работы ДКА
private static int processSymbol(char symbol) {

    int position = getSymbolPosition(symbol);

    if (position == UNKNOWN_SYMBOL_ERR) {
        return UNKNOWN_SYMBOL_ERR;
    }

    currentState =
            TRANSITION_TABLE[currentState][position];

    return OK;
}


// Проверка заключительного состояния
private static boolean isAccepted() {
    return FINAL_STATES[currentState];
}


// Возврат ДКА в начальное состояние
private static void reset() {
    currentState = DFA_STATES.q0.ordinal();
}


void main()
        throws IOException {

    setTransitions();
    reset();

    IO.println(
            "Enter a chain over alphabet {a, b}:"
    );

    boolean error = false;
    int code;

    /*
     * Ввод выполняется по одному символу.
     * Строка целиком не считывается.
     */
    while ((code = System.in.read()) != -1) {

        char symbol = (char) code;

        // В Windows Enter обычно даёт \r\n
        if (symbol == '\r') {
            continue;
        }

        // Конец входной цепочки
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