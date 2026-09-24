import automata.*;
import automata.fsa.*;
import file.XMLCodec;
import grammar.*;
import grammar.reg.*;
import gui.environment.RegularEnvironment;
import gui.regular.*;
import gui.viewer.AutomatonDrawer;
import pumping.*;
import regular.RegularExpression;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Reproducible construction and verification using the installed JFLAP 7.1 library. */
public class BuildVerifyPR2 {
    static final String RE = "(!+a+aa+aaa)(!+b+bb+bbb)";
    static final String OPTIONAL = "(!+a)(!+a)(!+a)(!+b)(!+b)(!+b)";
    static File root;
    static final XMLCodec codec = new XMLCodec();
    static final StringBuilder log = new StringBuilder();
    static final List<Automaton> gtgs = new ArrayList<>();
    static final List<String> gtgNames = new ArrayList<>();

    static void note(String s) { System.out.println(s); log.append(s).append('\n'); }
    static void write(String name, String text) throws Exception {
        File f = new File(root, name); f.getParentFile().mkdirs();
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }
    static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o);
    }
    static void save(String name, Serializable object) {
        File f = new File(root, name); f.getParentFile().mkdirs();
        if(object instanceof RegularPumpingLemma) {
            try {
                org.w3c.dom.Document doc=new file.xml.RegPumpingLemmaTransducer().toDOM(object);
                javax.xml.transform.TransformerFactory.newInstance().newTransformer().transform(
                    new javax.xml.transform.dom.DOMSource(doc),new javax.xml.transform.stream.StreamResult(f));
            } catch(Exception e){throw new RuntimeException(e);}
        } else codec.encode(object, f, new HashMap<Object,Object>());
        if (codec.decode(f, new HashMap<Object,Object>()) == null) throw new AssertionError(name);
    }
    static void snapshot(String name, Automaton a) {
        layoutDAG(a);save(name, a); gtgs.add((Automaton) a.clone()); gtgNames.add(name);
    }
    static void layoutDAG(Automaton a) {
        Map<State,Integer> rank=new HashMap<>();for(State s:a.getStates())rank.put(s,0);
        int gap=130;
        for(Transition t:a.getTransitions()){
            String label=((FSATransition)t).getLabel();if(!label.equals(FSAToRegularExpressionConverter.EMPTY))gap=Math.max(gap,label.length()*11+70);
        }
        for(int i=0;i<a.getStates().length;i++)for(Transition t:a.getTransitions()){
            if(t.getFromState()==t.getToState()||((FSATransition)t).getLabel().equals(FSAToRegularExpressionConverter.EMPTY))continue;
            rank.put(t.getToState(),Math.max(rank.get(t.getToState()),rank.get(t.getFromState())+1));
        }
        Map<Integer,List<State>> layers=new TreeMap<>();int max=1;
        for(State s:a.getStates())layers.computeIfAbsent(rank.get(s),k->new ArrayList<>()).add(s);
        for(List<State> row:layers.values())max=Math.max(max,row.size());
        for(Map.Entry<Integer,List<State>> e:layers.entrySet()){
            List<State> row=e.getValue();row.sort(Comparator.comparingInt(State::getID));
            for(int i=0;i<row.size();i++)row.get(i).setPoint(new Point(90+e.getKey()*gap,80+(max-row.size())*55+i*110));
        }
    }
    static State state(FiniteStateAutomaton a, int id, String name, int x, int y) {
        State s = a.createStateWithId(new Point(x,y), id); s.setName(name); return s;
    }
    static void edge(Automaton a, State s, State t, String label) {
        a.addTransition(new FSATransition(s,t,label));
    }
    static FiniteStateAutomaton convertRE(String expression, boolean snapshots) throws Exception {
        ConvertToAutomatonPane pane = new ConvertToAutomatonPane(new RegularEnvironment(new RegularExpression(expression)));
        FiniteStateAutomaton a = (FiniteStateAutomaton) field(pane,"automaton");
        REToFSAController c = (REToFSAController) field(pane,"controller");
        if (!snapshots) { c.completeAll(); return a; }
        snapshot("steps/re_to_nfa/step_00.jff", a);
        int step=0;
        StringBuilder table = new StringBuilder("step\tstates\ttransitions\n0\t2\t1\n");
        while (!((Set<?>)field(c,"toDo")).isEmpty()) {
            c.completeStep(); step++;
            snapshot(String.format("steps/re_to_nfa/step_%02d.jff",step), a);
            table.append(step).append('\t').append(a.getStates().length).append('\t').append(a.getTransitions().length).append('\n');
            if (step>100) throw new AssertionError("Conversion did not finish");
        }
        write("verification/re_steps.tsv",table.toString());
        note("Native RE -> NFA: "+step+" completed steps, "+a.getStates().length+" states, "+a.getTransitions().length+" transitions.");
        return a;
    }
    static FiniteStateAutomaton optionalNFA() {
        FiniteStateAutomaton a=new FiniteStateAutomaton(); State[] q=new State[7];
        for(int i=0;i<7;i++)q[i]=state(a,i,"q"+i,90+130*i,110);
        a.setInitialState(q[0]);a.addFinalState(q[6]);
        for(int i=0;i<6;i++){edge(a,q[i],q[i+1],i<3?"a":"b");edge(a,q[i],q[i+1],"");}
        return a;
    }
    static FiniteStateAutomaton dfa() {
        FiniteStateAutomaton a=new FiniteStateAutomaton();State[] q=new State[8];
        for(int i=0;i<4;i++)q[i]=state(a,i,i==0?"S":""+(char)('A'+i-1),100+180*i,100);
        for(int i=4;i<7;i++)q[i]=state(a,i,""+(char)('D'+i-4),280+180*(i-4),320);
        q[7]=state(a,7,"X",100,540);a.setInitialState(q[0]);
        for(int i=0;i<7;i++)a.addFinalState(q[i]);
        int[][] delta={{1,4},{2,4},{3,4},{7,4},{7,5},{7,6},{7,7},{7,7}};
        for(int i=0;i<8;i++){edge(a,q[i],q[delta[i][0]],"a");edge(a,q[i],q[delta[i][1]],"b");}
        return a;
    }
    static RegularGrammar grammar() {
        RightLinearGrammar g=new RightLinearGrammar();g.setStartVariable("S");
        String[][] rules={{"S","aA"},{"S","bD"},{"S",""},{"A","aB"},{"A","bD"},{"A",""},
            {"B","aC"},{"B","bD"},{"B",""},{"C","bD"},{"C",""},{"D","bE"},{"D",""},
            {"E","bF"},{"E",""},{"F",""}};
        for(String[] r:rules)g.addProduction(new Production(r[0],r[1]));
        return g;
    }
    static Set<String> grammarLanguage(Grammar g) {
        Set<String> result=new TreeSet<>(),seen=new HashSet<>();ArrayDeque<String> todo=new ArrayDeque<>();
        todo.add(g.getStartVariable());
        while(!todo.isEmpty()) {
            String s=todo.remove();if(!seen.add(s))continue;
            int pos=-1;for(int i=0;i<s.length();i++)if(Character.isUpperCase(s.charAt(i))){pos=i;break;}
            if(pos<0){result.add(s);continue;}
            for(Production p:g.getProductions())if(p.getLHS().equals(s.substring(pos,pos+1)))
                todo.add(s.substring(0,pos)+p.getRHS()+s.substring(pos+1));
            if(seen.size()>1000)throw new AssertionError("Unexpected grammar cycle");
        }
        return result;
    }
    static String grammarToRE(Automaton input) throws Exception {
        Automaton a=(Automaton)input.clone();
        FSAToRegularExpressionConverter.convertToSimpleAutomaton(a);
        snapshot("steps/grammar_to_re/step_00.jff",a);
        int step=0;StringBuilder order=new StringBuilder("step\tremoved_state\n");
        while(FSAToRegularExpressionConverter.areRemovableStates(a)) {
            State[] states=a.getStates();Arrays.sort(states,Comparator.comparingInt(State::getID));
            State pick=null;for(State s:states)if(FSAToRegularExpressionConverter.isRemovable(s,a)){pick=s;break;}
            if(pick==null)throw new AssertionError("No removable state");
            order.append(++step).append('\t').append(pick.getName()).append('\n');
            Transition[] replacements=FSAToRegularExpressionConverter.getTransitionsForRemoveState(pick,a);
            FSAToRegularExpressionConverter.removeState(pick,replacements,a);
            snapshot(String.format("steps/grammar_to_re/step_%02d.jff",step),a);
        }
        String expression=FSAToRegularExpressionConverter.getExpressionFromGTG(a);
        write("verification/grammar_to_re_steps.tsv",order.toString());
        write("verification/derived_expression.txt",expression+"\n");
        note("Native grammar NFA -> RE: "+step+" state eliminations; expression="+expression);
        return expression;
    }
    // Generalized transitions use RE semantics here, unlike ordinary FA input simulation.
    static boolean generalizedAccept(Automaton a,String word) {
        ArrayDeque<int[]> todo=new ArrayDeque<>();Set<String> seen=new HashSet<>();
        todo.add(new int[]{a.getInitialState().getID(),0});
        while(!todo.isEmpty()) {
            int[] c=todo.remove();if(!seen.add(c[0]+":"+c[1]))continue;
            State s=a.getStateWithID(c[0]);if(c[1]==word.length()&&a.isFinalState(s))return true;
            for(Transition tr:a.getTransitionsFromState(s)) {
                String label=((FSATransition)tr).getLabel();
                if(label.equals(FSAToRegularExpressionConverter.EMPTY))continue;
                String regex=label.replace("!", "").replace(FSAToRegularExpressionConverter.LAMBDA_DISPLAY, "").replace("+", "|");
                Pattern p=Pattern.compile(regex);
                for(int end=c[1];end<=word.length();end++)if(p.matcher(word.substring(c[1],end)).matches())
                    todo.add(new int[]{tr.getToState().getID(),end});
            }
        }
        return false;
    }
    static boolean condition(String w) {
        int n=0,m=0;boolean b=false;
        for(char c:w.toCharArray()) {if(c=='a'&&!b)n++;else if(c=='b'){b=true;m++;}else return false;}
        return n<4&&m<=3;
    }
    static void render(Automaton a,String name) throws Exception {
        AutomatonDrawer drawer=new AutomatonDrawer(a);Rectangle r=drawer.getBounds();
        int pad=50,w=r.width+2*pad,h=r.height+2*pad;
        BufferedImage im=new BufferedImage(w*2,h*2,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();
        g.setColor(Color.WHITE);g.fillRect(0,0,im.getWidth(),im.getHeight());g.scale(2,2);g.translate(pad-r.x,pad-r.y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        drawer.drawAutomaton(g);g.dispose();File f=new File(root,name);f.getParentFile().mkdirs();ImageIO.write(im,"png",f);
    }
    static void trainer() throws Exception {
        RegularPumpingLemma[] tasks={new pumping.reg.AnEven(),new pumping.reg.AnBn()};
        StringBuilder out=new StringBuilder("language\tp\tw\tx\ty\tz\ti\tpumped\tin_language\n");
        for(int t=0;t<tasks.length;t++) {
            RegularPumpingLemma p=tasks[t];p.setFirstPlayer(PumpingLemma.HUMAN);p.setM(t==0?2:4);
            p.setW(t==0?"aaaa":"aaaabbbb");
            if(!p.setDecomposition(t==0?new int[]{0,2}:new int[]{1,1}))throw new AssertionError("Trainer split");
            for(int i=0;i<=3;i++){
                p.setI(i);String pumped=p.createPumpedString();boolean member=p.isInLang(pumped);
                if(member!=(t==0||i==1))throw new AssertionError("Trainer result");
                out.append(p.getTitle()).append('\t').append(p.getM()).append('\t').append(p.getW()).append('\t')
                    .append(p.getX()).append('\t').append(p.getY()).append('\t').append(p.getZ()).append('\t')
                    .append(i).append('\t').append(pumped).append('\t').append(member).append('\n');
            }
            p.setI(0);save(t==0?"pumping/Example_regular_even_a.jff":"pumping/Example_nonregular_anbn.jff",p);
        }
        write("verification/trainer_results.tsv",out.toString());
        note("Native JFLAP pumping trainer: 2 examples, i=0..3, all expected results confirmed.");
    }
    static void pumpingWitness() throws Exception {
        StringBuilder out=new StringBuilder("p\tx_length\ty_length\ti\tn_after\tl\tk\tin_L29\n");int count=0;
        for(int p=1;p<=30;p++)for(int r=0;r<p;r++)for(int s=1;r+s<=p;s++){
            int n=p+s,l=p,k=p;boolean member=n==l||l!=k;
            if(member)throw new AssertionError("Pumping witness");
            out.append(p).append('\t').append(r).append('\t').append(s).append("\t2\t").append(n).append('\t')
                .append(l).append('\t').append(k).append("\tfalse\n");count++;
        }
        write("verification/L29_pumping_checks.tsv",out.toString());
        note("L29 illustration: "+count+" valid splits for p=1..30, pumping i=2 leaves the language (not a substitute for the proof).");
    }
    public static void main(String[] args) throws Exception {
        root=new File(args.length==0?".":args[0]);root.mkdirs();
        note("PR2, variant 3. JFLAP 7.1 native API verification. Date: 2026-09-24.");
        save("Variant3_RE.jff",new RegularExpression(RE));save("Variant3_RE_optional.jff",new RegularExpression(OPTIONAL));
        FiniteStateAutomaton raw=convertRE(RE,true);save("Variant3_NFA_from_RE.jff",raw);
        FiniteStateAutomaton compact=optionalNFA();save("Variant3_NFA_compact.jff",compact);render(compact,"images/NFA_compact.png");
        FiniteStateAutomaton deterministic=dfa();save("Variant3_DFA.jff",deterministic);
        RegularGrammar grammar=grammar();save("Variant3_Grammar.jff",grammar);
        FiniteStateAutomaton ga=new FiniteStateAutomaton();
        RightLinearGrammarToFSAConverter gc=new RightLinearGrammarToFSAConverter();
        gc.initialize();gc.createStatesForConversion(grammar,ga);
        for(Production p:grammar.getProductions())ga.addTransition(gc.getTransitionForProduction(p));
        String[] variables={"S","A","B","C","D","E","F"};
        for(int j=0;j<variables.length;j++){
            State s=gc.getStateForVariable(variables[j]);s.setName(variables[j]);s.setLabel("");
            s.setPoint(j<4?new Point(100+180*j,100):new Point(280+180*(j-4),300));
        }
        State terminal=ga.getFinalStates()[0];terminal.setName("T");terminal.setPoint(new Point(1100,700));
        save("Variant3_NFA_from_Grammar.jff",ga);
        render(ga,"images/NFA_from_Grammar.png");
        String derived=grammarToRE(ga);save("Variant3_RE_from_Grammar.jff",new RegularExpression(derived));
        Automaton derivedNfa=convertRE(derived,false);
        Grammar decoded=(Grammar)codec.decode(new File(root,"Variant3_Grammar.jff"),new HashMap<Object,Object>());
        Set<String> generated=grammarLanguage(decoded);if(generated.size()!=16)throw new AssertionError("Grammar size");
        StringBuilder accepted=new StringBuilder();for(String w:generated)accepted.append(w.isEmpty()?"EPS":w).append('\n');
        write("verification/grammar_language.txt",accepted.toString());
        String[] names={"NFA_from_RE","NFA_compact","DFA","NFA_from_Grammar"};
        List<Automaton> models=new ArrayList<>();
        for(String name:names)models.add((Automaton)codec.decode(new File(root,"Variant3_"+name+".jff"),new HashMap<Object,Object>()));
        models.add(derivedNfa);int words=0;
        StringBuilder tests=new StringBuilder("word\texpected\tNFA_RE\tNFA_compact\tDFA\tNFA_grammar\tRE_grammar\tgrammar\n");
        for(int len=0;len<=10;len++)for(int bits=0;bits<(1<<len);bits++){
            StringBuilder w=new StringBuilder();for(int j=len-1;j>=0;j--)w.append(((bits>>j)&1)==0?'a':'b');
            String word=w.toString();boolean want=condition(word);words++;
            tests.append(word.isEmpty()?"EPS":word).append('\t').append(want);
            for(Automaton a:models){boolean got=new FSAStepByStateSimulator(a).simulateInput(word);if(got!=want)throw new AssertionError("FA mismatch "+word);tests.append('\t').append(got);}
            if(generated.contains(word)!=want)throw new AssertionError("Grammar mismatch");tests.append('\t').append(want).append('\n');
        }
        write("verification/exhaustive_results.tsv",tests.toString());
        note("Language checks: "+words+" words of length 0..10, 5 automata and grammar, zero mismatches.");
        // Validate each intermediate graph on all words through length 7 using RE-edge semantics.
        int intermediate=0;
        for(int model=0;model<gtgs.size();model++)for(int len=0;len<=7;len++)for(int bits=0;bits<(1<<len);bits++){
            StringBuilder w=new StringBuilder();for(int j=len-1;j>=0;j--)w.append(((bits>>j)&1)==0?'a':'b');
            if(generalizedAccept(gtgs.get(model),w.toString())!=condition(w.toString()))throw new AssertionError("GTG "+gtgNames.get(model)+" "+w);
            intermediate++;
        }
        note("Intermediate generalized graphs: "+gtgs.size()+", "+intermediate+" RE-semantics checks, zero mismatches.");
        trainer();pumpingWitness();write("verification/verification.txt",log.toString());
    }
}

