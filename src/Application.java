

import java.sql.*;
import java.util.*;
import java.util.Arrays;

public class Application {

    private static Scanner scan = new Scanner(System.in);

    private static ArrayList<Word> initialCollection = new ArrayList<>();
    private static HashMap<String, Word> initialHashMap = new HashMap<>();
    private static ArrayList<Word> importantWords = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/Dictionary?useSSL=false", "root", "password");

        System.out.println("Going through Database");
        inputFromDb(conn, true);

        System.out.println("Going through definitions");
        connectDefinitions();

        System.out.println("Determining which words are used to define other words");
        int count = removeDerivatives();

        System.out.println("Reduced from " + count + " to " + initialCollection.size());
        addCollectionToDb(conn, "setOfWordsUsedToDefine", initialCollection);

        System.out.println("Finding top words");
        sortByImportantWords(true);

        System.out.println("Reduced from " + count + " to " + importantWords.size());
        addCollectionToDb(conn, "importantWords", importantWords);

    }

    public static void inputFromDb(Connection conn, boolean consoleOutput) {
        try {
            PreparedStatement prep = conn.prepareStatement("Select * from entries");
            ResultSet resultSet = prep.executeQuery();
            char first = '0';
            while (resultSet.next()) {
                String name = Word.sanitiseWord(resultSet.getString(1));
                String definition = resultSet.getString(3);
                if (initialHashMap.get(name) == null) {
                    Word word = new Word(name, definition);
                    if (consoleOutput && name.toCharArray()[0] != first) {
                        first = name.toCharArray()[0];
                        System.out.print(first);
                    }
                    initialCollection.add(word);
                    initialHashMap.put(name, word);
                } else {
                    Word.allWords.stream().filter(i -> i.getName().equals(name)).forEach(i -> i.addToDefinition(definition));
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.exit(420);
        }
        System.out.println();
    }

    public static void connectDefinitions() {
        initialCollection.stream().forEach(w -> {
            Arrays.stream(w.getDefinition()).forEach(s -> {
                try {
                    w.addToDefinitionWords(initialHashMap.get(s).addToWordDefinedByThis(w));
                } catch (Exception e) {
                    if (s.endsWith("s")) {
                        s = s.substring(0, s.length() - 1);
                        try {
                            w.addToDefinitionWords(initialHashMap.get(s).addToWordDefinedByThis(w));
                        } catch (Exception ee) {
                            if (s.endsWith("e")) {
                                s = s.substring(0, s.length() - 1);
                                try {
                                    w.addToDefinitionWords(initialHashMap.get(s).addToWordDefinedByThis(w));
                                } catch (Exception eee) {
                                }
                            }
                        }
                    }
                }
            });
        });
    }

    public static int removeDerivatives() {
        int count = initialCollection.size();
        ArrayList<Word> toRemove = new ArrayList<>();
        for (Word w : initialCollection) {
            if (w.timesUsedWeight() == 0) {
                toRemove.add(w);
            }
        }
        initialCollection.removeAll(toRemove);
        return count;
    }

    public static void addCollectionToDb(Connection conn, String table, ArrayList<Word> collection) {
        try {
            PreparedStatement p = conn.prepareStatement("drop table " + table);
            p.executeUpdate();
        } catch (Exception e) {
            System.out.println("Table " + table + " does not exist");
        }
        try {
            PreparedStatement p = conn.prepareStatement("create table if not exists `" + table + "` (`Word` VARCHAR(32), `Definition` TEXT)");
            p.executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(420);
        }

        collection.stream().forEach(i -> {
            try {
                String def = "";
                for (String s : i.getDefinition()) {
                    def += s;
                    def += " ";
                }
                PreparedStatement prepstmt = conn.prepareStatement("insert into " + table + " values ('" + i.getName() + "','" + def + "')");
                prepstmt.executeUpdate();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
    }

    public static void sortByImportantWords(boolean consoleOutput) {
        int i = 0;
        do {
            i++;
            long topWord = 0;
            Word top = null;
            for (Word w : initialCollection) {
                long gdws = w.getDefinitionWordsSum();
                if (gdws > topWord) {
                    top = w;
                    topWord = gdws;
                }
            }
            importantWords.add(top);
            top.setFinalWord(true);
            initialCollection.remove(top);
            if (consoleOutput) {
                System.out.println(i + ": { " + top.getName() + " } { " + top.timesUsedWeight() + " } { " + topWord + " }");
            }
        } while (initialCollection.size() != 0);
    }
}