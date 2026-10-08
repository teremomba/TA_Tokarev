import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Практическая работа № 4, вариант 15. Java 8+, без внешних библиотек.
 * CYK: КСГ в НФХ. Эрли: произвольная КСГ с односимвольными обозначениями.
 * Оба алгоритма восстанавливают одно дерево для принятой цепочки.
 */
public final class SyntaxAnalyzer {
    private SyntaxAnalyzer() {
    }

    private static final class Rule {
        final char left;
        final String right;

        Rule(char left, String right) {
            this.left = left;
            this.right = right;
        }
    }

    private static final class Grammar {
        final char start;
        final List<Rule> rules;
        final Set<Character> variables;
        final Map<Character, List<Integer>> byLeft;

        Grammar(List<Rule> rules) {
            if (rules.isEmpty()) {
                throw new IllegalArgumentException("Grammar has no productions.");
            }
            this.rules = Collections.unmodifiableList(new ArrayList<Rule>(rules));
            start = rules.get(0).left;
            variables = new LinkedHashSet<Character>();
            byLeft = new LinkedHashMap<Character, List<Integer>>();
            for (int i = 0; i < rules.size(); i++) {
                Rule rule = rules.get(i);
                variables.add(rule.left);
                List<Integer> indexes = byLeft.get(rule.left);
                if (indexes == null) {
                    indexes = new ArrayList<Integer>();
                    byLeft.put(rule.left, indexes);
                }
                indexes.add(i);
            }
            for (Rule rule : rules) {
                for (int i = 0; i < rule.right.length(); i++) {
                    char symbol = rule.right.charAt(i);
                    if (isUpperVariable(symbol) && !variables.contains(symbol)) {
                        throw new IllegalArgumentException(
                                "Undefined variable: " + symbol);
                    }
                }
            }
        }

        static Grammar read(String path) throws Exception {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(Paths.get(path).toFile());
            Element root = document.getDocumentElement();
            NodeList types = root.getElementsByTagName("type");
            if (!"structure".equals(root.getTagName()) || types.getLength() != 1
                    || !"grammar".equals(types.item(0).getTextContent().trim())) {
                throw new IllegalArgumentException("Expected a JFLAP grammar (.jff).");
            }
            List<Rule> rules = new ArrayList<Rule>();
            NodeList productions = root.getElementsByTagName("production");
            for (int i = 0; i < productions.getLength(); i++) {
                Element production = (Element) productions.item(i);
                NodeList leftNodes = production.getElementsByTagName("left");
                NodeList rightNodes = production.getElementsByTagName("right");
                if (leftNodes.getLength() != 1 || rightNodes.getLength() != 1) {
                    throw new IllegalArgumentException("Invalid production element.");
                }
                String left = leftNodes.item(0).getTextContent();
                String right = rightNodes.item(0).getTextContent();
                if (left.length() != 1 || !isUpperVariable(left.charAt(0))) {
                    throw new IllegalArgumentException(
                            "Left sides must be single uppercase letters A-Z.");
                }
                rules.add(new Rule(left.charAt(0), right));
            }
            return new Grammar(rules);
        }

        void requireCnf() {
            for (Rule rule : rules) {
                boolean terminal = rule.right.length() == 1
                        && !variables.contains(rule.right.charAt(0));
                boolean binary = rule.right.length() == 2
                        && variables.contains(rule.right.charAt(0))
                        && variables.contains(rule.right.charAt(1));
                boolean startEmpty = rule.left == start && rule.right.isEmpty();
                if (!terminal && !binary && !startEmpty) {
                    throw new IllegalArgumentException(
                            "CYK requires CNF: invalid rule " + rule.left + " -> " + rule.right);
                }
            }
            boolean hasEmptyStart = false;
            for (Rule rule : rules) {
                hasEmptyStart |= rule.left == start && rule.right.isEmpty();
            }
            if (hasEmptyStart) {
                for (Rule rule : rules) {
                    if (rule.right.indexOf(start) >= 0) {
                        throw new IllegalArgumentException(
                                "In CNF with S -> eps, S cannot occur on right sides.");
                    }
                }
            }
        }
    }

    private static boolean isUpperVariable(char symbol) {
        return symbol >= 'A' && symbol <= 'Z';
    }

    private static final class Tree {
        final String label;
        final boolean nonterminal;
        final List<Tree> children;

        Tree(String label, boolean nonterminal, List<Tree> children) {
            this.label = label;
            this.nonterminal = nonterminal;
            this.children = Collections.unmodifiableList(new ArrayList<Tree>(children));
        }

        static Tree terminal(char symbol) {
            return new Tree(String.valueOf(symbol), false, Collections.<Tree>emptyList());
        }

        static Tree variable(char symbol, List<Tree> children) {
            return new Tree(String.valueOf(symbol), true, children);
        }

        void print(String prefix, boolean last) {
            System.out.println(prefix + (last ? "`-- " : "+-- ") + label);
            String childPrefix = prefix + (last ? "    " : "|   ");
            if (nonterminal && children.isEmpty()) {
                System.out.println(childPrefix + "`-- eps");
            }
            for (int i = 0; i < children.size(); i++) {
                children.get(i).print(childPrefix, i == children.size() - 1);
            }
        }
    }

    private static final class CykLink {
        final Rule rule;
        final int split;

        CykLink(Rule rule, int split) {
            this.rule = rule;
            this.split = split;
        }
    }

    private static final class CykResult {
        final Map<Character, CykLink>[][] table;
        final Tree tree;

        CykResult(Map<Character, CykLink>[][] table, Tree tree) {
            this.table = table;
            this.tree = tree;
        }

        void printTable(String input) {
            System.out.println("CYK table T[i,length], positions start at 1:");
            for (int length = 1; length <= input.length(); length++) {
                StringBuilder row = new StringBuilder("length " + length + ": ");
                for (int i = 0; i + length <= input.length(); i++) {
                    if (i != 0) {
                        row.append("; ");
                    }
                    row.append("T[").append(i + 1).append(',').append(length)
                            .append("]=").append(table[i][length].keySet());
                }
                System.out.println(row);
            }
            if (input.isEmpty()) {
                System.out.println("Empty input: accepted iff the grammar has S -> eps.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static CykResult cyk(Grammar grammar, String input) {
        int n = input.length();
        Map<Character, CykLink>[][] table =
                (Map<Character, CykLink>[][]) new Map<?, ?>[n][n + 1];
        List<Rule> binaryRules = new ArrayList<Rule>();
        Map<Character, List<Rule>> terminalRules = new LinkedHashMap<Character, List<Rule>>();
        for (Rule rule : grammar.rules) {
            if (rule.right.length() == 2) {
                binaryRules.add(rule);
            } else if (rule.right.length() == 1) {
                char terminal = rule.right.charAt(0);
                List<Rule> matching = terminalRules.get(terminal);
                if (matching == null) {
                    matching = new ArrayList<Rule>();
                    terminalRules.put(terminal, matching);
                }
                matching.add(rule);
            } else if (n == 0 && rule.left == grammar.start) {
                return new CykResult(table,
                        Tree.variable(grammar.start, Collections.<Tree>emptyList()));
            }
        }
        for (int i = 0; i < n; i++) {
            for (int length = 1; i + length <= n; length++) {
                table[i][length] = new LinkedHashMap<Character, CykLink>();
            }
            List<Rule> matching = terminalRules.get(input.charAt(i));
            if (matching != null) {
                for (Rule rule : matching) {
                    table[i][1].put(rule.left, new CykLink(rule, 0));
                }
            }
        }
        // В T[i,length] помещаются все нетерминалы, выводящие данную подстроку.
        for (int length = 2; length <= n; length++) {
            for (int i = 0; i + length <= n; i++) {
                for (int split = 1; split < length; split++) {
                    for (Rule rule : binaryRules) {
                        if (table[i][split].containsKey(rule.right.charAt(0))
                                && table[i + split][length - split].containsKey(
                                        rule.right.charAt(1))
                                && !table[i][length].containsKey(rule.left)) {
                            table[i][length].put(rule.left, new CykLink(rule, split));
                        }
                    }
                }
            }
        }
        Tree tree = n > 0 && table[0][n].containsKey(grammar.start)
                ? cykTree(table, 0, n, grammar.start) : null;
        return new CykResult(table, tree);
    }

    /** REDUCE из лекции: восстановление дерева по правилу и разбиению ячейки. */
    private static Tree cykTree(Map<Character, CykLink>[][] table,
                                int i, int length, char variable) {
        CykLink link = table[i][length].get(variable);
        List<Tree> children;
        if (length == 1) {
            children = Collections.singletonList(Tree.terminal(link.rule.right.charAt(0)));
        } else {
            children = Arrays.asList(
                    cykTree(table, i, link.split, link.rule.right.charAt(0)),
                    cykTree(table, i + link.split, length - link.split,
                            link.rule.right.charAt(1)));
        }
        return Tree.variable(variable, children);
    }

    /** Ситуация Эрли: номер продукции, положение точки, начало подстроки. */
    private static final class Item {
        final int rule;
        final int dot;
        final int origin;
        final List<Tree> children;

        Item(int rule, int dot, int origin, List<Tree> children) {
            this.rule = rule;
            this.dot = dot;
            this.origin = origin;
            this.children = Collections.unmodifiableList(new ArrayList<Tree>(children));
        }

        Item advance(Tree child) {
            List<Tree> nextChildren = new ArrayList<Tree>(children);
            nextChildren.add(child);
            return new Item(rule, dot + 1, origin, nextChildren);
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Item)) {
                return false;
            }
            Item other = (Item) object;
            return rule == other.rule && dot == other.dot && origin == other.origin;
        }

        @Override
        public int hashCode() {
            return (31 * rule + dot) * 31 + origin;
        }
    }

    private static final class Column {
        final Set<Item> items = new LinkedHashSet<Item>();
        final List<Item> agenda = new ArrayList<Item>();
        final Map<Character, List<Item>> waiting = new LinkedHashMap<Character, List<Item>>();
        final Map<Character, Tree> emptyCompleted = new LinkedHashMap<Character, Tree>();

        void add(Item item) {
            if (items.add(item)) {
                agenda.add(item);
            }
        }
    }

    private static final class EarleyResult {
        final List<Column> chart;
        final List<Rule> rules;
        final Tree tree;

        EarleyResult(List<Column> chart, List<Rule> rules, Tree tree) {
            this.chart = chart;
            this.rules = rules;
            this.tree = tree;
        }

        void printChart(String input) {
            System.out.println("Earley chart for #" + input + "#; S' -> "
                    + rules.get(0).right + ":");
            for (int i = 0; i < chart.size(); i++) {
                System.out.println("M[" + i + "] (" + chart.get(i).items.size() + " items):");
                for (Item item : chart.get(i).items) {
                    Rule rule = rules.get(item.rule);
                    String left = item.rule == 0 ? "S'" : String.valueOf(rule.left);
                    System.out.println("  [" + left + " -> "
                            + rule.right.substring(0, item.dot) + "."
                            + rule.right.substring(item.dot) + ", " + item.origin + "]");
                }
            }
        }
    }

    /** Бесконтекстный Эрли: предсказатель, считыватель, завершатель. */
    private static EarleyResult earley(Grammar grammar, String input) {
        String bounded = "#" + input + "#";
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule('@', "#" + grammar.start + "#"));
        rules.addAll(grammar.rules);
        List<Column> chart = new ArrayList<Column>();
        for (int i = 0; i <= bounded.length(); i++) {
            chart.add(new Column());
        }
        chart.get(0).add(new Item(0, 0, 0, Collections.<Tree>emptyList()));
        for (int i = 0; i <= bounded.length(); i++) {
            Column current = chart.get(i);
            // Новые ситуации добавляются в конец списка; каждая обрабатывается один раз.
            for (int cursor = 0; cursor < current.agenda.size(); cursor++) {
                Item item = current.agenda.get(cursor);
                Rule rule = rules.get(item.rule);
                if (item.dot == rule.right.length()) {
                    // Завершатель: A выведен из подстроки [origin,i).
                    Tree completed = Tree.variable(rule.left, item.children);
                    if (item.origin == i && !current.emptyCompleted.containsKey(rule.left)) {
                        current.emptyCompleted.put(rule.left, completed);
                    }
                    List<Item> parents = chart.get(item.origin).waiting.get(rule.left);
                    if (parents != null) {
                        for (Item parent : new ArrayList<Item>(parents)) {
                            current.add(parent.advance(completed));
                        }
                    }
                } else {
                    char next = rule.right.charAt(item.dot);
                    if (grammar.variables.contains(next)) {
                        List<Item> parents = current.waiting.get(next);
                        if (parents == null) {
                            parents = new ArrayList<Item>();
                            current.waiting.put(next, parents);
                        }
                        parents.add(item);
                        // Предсказатель: все продукции ожидаемого нетерминала.
                        for (Integer index : grammar.byLeft.get(next)) {
                            current.add(new Item(index + 1, 0, i,
                                    Collections.<Tree>emptyList()));
                        }
                        // Если пустой вывод уже завершён, новый родитель тоже продвигается.
                        // Это устраняет зависимость обработки epsilon-правил от порядка.
                        Tree empty = current.emptyCompleted.get(next);
                        if (empty != null) {
                            current.add(item.advance(empty));
                        }
                    } else if (i < bounded.length() && next == bounded.charAt(i)) {
                        // Считыватель: совпавший терминал переносит ситуацию в M[i+1].
                        chart.get(i + 1).add(item.advance(Tree.terminal(next)));
                    }
                }
            }
        }
        Tree tree = null;
        for (Item item : chart.get(bounded.length()).items) {
            if (item.rule == 0 && item.dot == 3 && item.origin == 0) {
                // Средний ребёнок дополненного правила - дерево исходной грамматики.
                tree = item.children.get(1);
                break;
            }
        }
        return new EarleyResult(chart, rules, tree);
    }

    private static String decodeInput(String input) {
        return "eps".equals(input) || "\u03b5".equals(input) ? "" : input;
    }

    private static void analyze(String algorithm, Grammar grammar,
                                String input, boolean trace) {
        Tree tree;
        if ("cyk".equals(algorithm)) {
            CykResult result = cyk(grammar, input);
            tree = result.tree;
            if (trace) {
                result.printTable(input);
            }
        } else {
            EarleyResult result = earley(grammar, input);
            tree = result.tree;
            if (trace) {
                result.printChart(input);
            }
        }
        if (trace && tree != null) {
            System.out.println("Parse tree:");
            tree.print("", true);
        }
        System.out.println("Input: " + (input.isEmpty() ? "eps" : input)
                + " | Result: " + (tree != null ? "Accept" : "Reject"));
    }

    public static void main(String[] args) {
        if (args.length < 2 || !("cyk".equals(args[0]) || "earley".equals(args[0]))) {
            System.err.println("Usage: java -cp out SyntaxAnalyzer cyk|earley grammar.jff"
                    + " [--trace] [strings...]");
            System.exit(1);
        }
        try {
            String algorithm = args[0];
            Grammar grammar = Grammar.read(args[1]);
            if ("cyk".equals(algorithm)) {
                grammar.requireCnf();
            }
            boolean trace = args.length > 2 && "--trace".equals(args[2]);
            int firstInput = trace ? 3 : 2;
            System.out.println("Algorithm: " + ("cyk".equals(algorithm) ? "CYK" : "Earley"));
            System.out.println("Grammar: " + args[1] + " | Start: " + grammar.start);
            if (firstInput < args.length) {
                for (int i = firstInput; i < args.length; i++) {
                    analyze(algorithm, grammar, decodeInput(args[i]), trace);
                }
            } else {
                System.out.println("Enter one string per line. Empty line = eps. Type exit to quit.");
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(System.in, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    if ("exit".equalsIgnoreCase(line.trim())) {
                        break;
                    }
                    analyze(algorithm, grammar, decodeInput(line), trace);
                }
            }
        } catch (Exception exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }
}
