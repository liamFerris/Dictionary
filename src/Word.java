import java.util.ArrayList;
import java.util.Arrays;

public class Word {

    public static ArrayList<Word> allWords = new ArrayList<>();

    private String name;
    private String[] definition;
    private ArrayList<Word> definitionWords = new ArrayList<>();
    private ArrayList<Word> wordsDefinedByThis = new ArrayList<>();
    private boolean finalWord = false;

    public String getName() {
        return name;
    }

    public String[] getDefinition() {
        return definition;
    }

    public void setFinalWord(boolean finalWord) {
        this.finalWord = finalWord;
    }

    public long timesUsedWeight() {
        return wordsDefinedByThis.stream().filter(i -> !i.finalWord).count();
    }

    public Word addToWordDefinedByThis(Word word) {
        if (!wordsDefinedByThis.contains(word) && !this.equals(word)) {
            wordsDefinedByThis.add(word);
        }
        return this;
    }

    public long getDefinitionWordsSum() {
        return definitionWords.stream().filter(i -> !i.finalWord).mapToLong(Word::timesUsedWeight).sum();
    }

    public static String sanitiseWord(String word) {
        return word.replaceAll("[^a-zA-Z ]", "").toLowerCase().replace(" ","");
    }

    //TODO public String deriveWord(){}

    public Word(String name, String definition) {
        this.name = sanitiseWord(name);
        this.definition = sanitiseDefinition(definition);
        allWords.add(this);
    }

    public void addToDefinition(String definition) {
        this.definition = concat(this.definition, sanitiseDefinition(definition));
    }

    public void addToDefinitionWords(Word word) {
        if (!definitionWords.contains(word) && !this.equals(word)) {
            definitionWords.add(word);
        }
    }

    private String[] sanitiseDefinition(String definition) {
        return definition.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
    }

    private static String[] concat(String[] first, String[] second) {
        String[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}