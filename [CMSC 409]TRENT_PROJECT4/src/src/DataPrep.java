import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataPrep {
    private final static String BASE_SENTENCE_FILENAME = "src/sentences.txt";
    private final static String STOP_WORDS_FILENAME = "src/stop_words.txt";
    private final static String PRE_STEM_OUTPUT_FILENAME = "src/sentencesPreStem.txt";
    private final static String POST_STEM_OUTPUT_FILENAME = "src/sentencesPostStem.txt";
    private final static String TDM_FILENAME = "src/TDM.txt";

    private final static String[] SPELLED_NUMS_ARRAY =
            {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};

    private static final Set<String> SPELLED_NUMS = new HashSet<>(Arrays.asList(SPELLED_NUMS_ARRAY));

    private static final Character[] SPECIAL_CHARS_ARRAY =
            {',', '(', ')', '"', '"', '“', '”', '\''};
    private static final Set<Character> SPECIAL_CHARS = new HashSet<>(Arrays.asList(SPECIAL_CHARS_ARRAY));

    private static Set<String> stopWords;

    private static boolean isNumber(String word) {
        if (word.contains(".")) {
            if (!word.substring(word.length() - 1).equals(".")) {
                String[] parts = word.split("\\.");
                if (parts.length < 3) {
                    if (parts.length == 0) {
                        return false;
                    }
                    if (StringUtils.isNumeric(parts[0]) && StringUtils.isNumeric(parts[1])) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (StringUtils.isNumeric(word)) {
            return true;
        } else if (SPELLED_NUMS.contains(word)) {
            return true;
        }
        return false;
    }

    private static boolean isEndOfSentence(String word) {
        if (word.contains(".")) {
            if (word.substring(word.length() - 1).equals(".")) {
                return true;
            }
            return false;
        }
        return false;
    }

    private static boolean isEndOfSentenceNumber(String endWord) {
        String word = endWord.substring(0, endWord.length() - 1);
        if (StringUtils.isNumeric(word)) {
            return true;
        } else {
            return false;
        }
    }

    private static String removeSpecialCharacters(String word) {
        String fixedWord = "";
        for (int i = 0; i < word.length(); i++) {
            Character c = word.charAt(i);
            if (!SPECIAL_CHARS.contains(c)) {
                fixedWord += c;
            }
        }
        return fixedWord;
    }

    //Assures every result has at least 2 entries
    private static String[] splitHyphenated(String word) {
        String[] result;
        if (word.charAt(0) == '-') {
            return new String[]{word.substring(1, word.length()), ""};
        } else if (word.charAt(word.length() - 1) == '-') {
            return new String[]{word.substring(0, word.length() - 1), ""};
        } else {
            return word.split("-");
        }
    }

    private static boolean isWord(String word) {
        if (word.length() > 1) {
            return true;
        }
        return false;
    }

    private static boolean isStopWord(String word) {
        return stopWords.contains(word);
    }

    private static void stemSentences(String preStemData, String postStemFileName) {
        char[] w = new char[501];
        Stemmer s = new Stemmer();
        try {
            FileInputStream in = new FileInputStream(preStemData);
            PrintWriter writer = new PrintWriter(postStemFileName);
            try {
                while (true) {
                    int ch = in.read();
                    if (Character.isLetter((char) ch)) {
                        int j = 0;
                        while (true) {
                            ch = Character.toLowerCase((char) ch);
                            w[j] = (char) ch;
                            if (j < 500) j++;
                            ch = in.read();
                            if (!Character.isLetter((char) ch)) {
                                for (int c = 0; c < j; c++) s.add(w[c]);
                                s.stem();
                                {
                                    String u;
                                    u = s.toString();
                                    writer.print(u);
                                }
                                break;
                            }
                        }
                    }
                    if (ch < 0) break;
                    writer.print((char) ch);
                }
                writer.close();
            } catch (IOException e) {
                System.out.println("error reading " + preStemData);
            }
        } catch (FileNotFoundException e) {
            System.out.println("file " + preStemData + " not found");
        }
    }

    public static void combine(String remove, String keep, Map<String, Integer> stems, Map<String, String> combined){
        int numRemove = stems.get(remove);
        int sum = stems.get(keep) + numRemove;
        stems.remove(remove);
        stems.replace(keep, sum);
        combined.put(remove, keep);
    }

    public static int[][] sendTDM(){
        try {
            File stopWordsFile = new File(STOP_WORDS_FILENAME);
            Scanner stopWordsScanner = new Scanner(stopWordsFile);
            stopWords = new HashSet<>();
            while (stopWordsScanner.hasNext()) {
                stopWords.add(stopWordsScanner.next());
            }
            stopWordsScanner.close();

            File file = new File(BASE_SENTENCE_FILENAME);
            Scanner scanner = new Scanner(file);
            String sentence = "";
            ArrayList<String> sentenceList = new ArrayList<>();

            while (scanner.hasNext()) {
                String word = scanner.next().toLowerCase();
                word = removeSpecialCharacters(word);
                word = word.trim();

                //Hyphenated
                if (word.contains("-")) {
                    String[] hyphen = splitHyphenated(word);
                    String word1 = hyphen[0];
                    String word2 = hyphen[1];
                    word1 = removeSpecialCharacters(word1);
                    word1 = word1.trim();
                    word2 = removeSpecialCharacters(word2);
                    word2 = word2.trim();

                    if (!isNumber(word1)) {
                        if (isWord(word1) && !isStopWord(word1)) {
                            sentence += (word1 + " ");
                        }
                    }

                    if (!isNumber(word2)) {
                        if (isEndOfSentence(word2)) {
                            if (isEndOfSentenceNumber(word2)) {
                                sentenceList.add(sentence);
                                sentence = "";
                            } else {
                                if (isWord(word2) && !isStopWord(word2)) {
                                    sentence += word2.substring(0, word2.length() - 1);
                                    sentenceList.add(sentence);
                                    sentence = "";
                                }
                            }
                        } else {
                            if (isWord(word2) && !isStopWord(word2)) {
                                sentence += (word2 + " ");
                            }
                        }
                    }
                }
                //Not Hyphenated
                else {
                    if (!isNumber(word)) {
                        if (isEndOfSentence(word)) {
                            if (isEndOfSentenceNumber(word)) {
                                sentenceList.add(sentence);
                                sentence = "";
                            } else {
                                if (isWord(word) && !isStopWord(word)) {
                                    sentence += word.substring(0, word.length() - 1);
                                    sentenceList.add(sentence);
                                    sentence = "";
                                }
                            }
                        } else {
                            if (isWord(word) && !isStopWord(word)) {
                                sentence += (word + " ");
                            }
                        }
                    }
                }
            }
            scanner.close();

            PrintWriter writer = new PrintWriter(PRE_STEM_OUTPUT_FILENAME);
            for (String line : sentenceList) {
                writer.println(line);
            }
            writer.close();

            stemSentences(PRE_STEM_OUTPUT_FILENAME, POST_STEM_OUTPUT_FILENAME);

            File stemmedSentencesFile = new File(POST_STEM_OUTPUT_FILENAME);
            //Map<String, Integer> stems = new HashMap<>();
            Map<String, Integer> stems = new TreeMap<>();
            scanner = new Scanner(stemmedSentencesFile);
            while (scanner.hasNext()){
                String stemWord = scanner.next();
                if (stems.containsKey(stemWord)){
                    int count = stems.get(stemWord);
                    count++;
                    stems.replace(stemWord, count);
                }
                else{
                    stems.put(stemWord, 1);
                }
            }
            scanner.close();

            Map<String, String> combinedWords = new HashMap<>();
            combine("townhous", "hous", stems, combinedWords);
            combine("hous", "home", stems, combinedWords);
            combine("kilomet", "mile", stems, combinedWords);
            combine("renov", "remodel", stems, combinedWords);
            combine("describ", "express", stems, combinedWords);
            combine("bed", "bedroom", stems, combinedWords);
            combine("bath", "bathroom", stems, combinedWords);
            combine("drive", "driven", stems, combinedWords);
            combine("sens", "feel", stems, combinedWords);
            combine("sedan", "car", stems, combinedWords);
            combine("anim", "pet", stems, combinedWords);
            combine("achiev", "attain", stems, combinedWords);
            combine("central", "major", stems, combinedWords);
            combine("complet", "finish", stems, combinedWords);
            combine("nice", "great", stems, combinedWords);
            combine("awar", "sentienc", stems, combinedWords);
            combine("trash", "sewag", stems, combinedWords);
            combine("sewag", "water", stems, combinedWords);
            combine("through", "via", stems, combinedWords);
            combine("recent", "new", stems, combinedWords);
            combine("newli", "freshli", stems, combinedWords);
            combine("area", "space", stems, combinedWords);
            combine("rang", "spread", stems, combinedWords);
            combine("sort", "type", stems, combinedWords);
            combine("biolog", "human", stems, combinedWords);
            combine("go", "went", stems, combinedWords);
            combine("round", "around", stems, combinedWords);
            combine("realiti", "world", stems, combinedWords);
            combine("queen", "king", stems, combinedWords);
            combine("rent", "tenant", stems, combinedWords);
            combine("tenant", "owner", stems, combinedWords);
            combine("combin", "merger", stems, combinedWords);
            combine("increas", "improv", stems, combinedWords);
            combine("dryer", "washer", stems, combinedWords);
            combine("john", "mccarthi", stems, combinedWords);
            combine("mccarthi", "inventor", stems, combinedWords);
            combine("rai", "kurzweil", stems, combinedWords);
            combine("kurzweil", "author", stems, combinedWords);
            combine("get", "attain", stems, combinedWords);
            combine("knowledg", "know", stems, combinedWords);
            combine("wai", "rout", stems, combinedWords);
            combine("entir", "full", stems, combinedWords);
            combine("engag", "work", stems, combinedWords);
            combine("gener", "common", stems, combinedWords);
            combine("comput", "machin", stems, combinedWords);
            combine("number", "multipl", stems, combinedWords);
            combine("fundament", "groundwork", stems, combinedWords);
            combine("lisp", "languag", stems, combinedWords);
            combine("suit", "room", stems, combinedWords);
            combine("travel", "went", stems, combinedWords);
            combine("deal", "work", stems, combinedWords);
            combine("pound", "gallon", stems, combinedWords);
            combine("eat", "kitchen", stems, combinedWords);
            combine("rout", "road", stems, combinedWords);
            combine("heat", "util", stems, combinedWords);
            combine("ga", "util", stems, combinedWords);
            combine("air", "util", stems, combinedWords);
            combine("water", "util", stems, combinedWords);
            combine("electr", "util", stems, combinedWords);
            combine("famili", "pet", stems, combinedWords);
            combine("bathroom", "room", stems, combinedWords);
            combine("bedroom", "room", stems, combinedWords);

            writer = new PrintWriter(TDM_FILENAME);
            ArrayList<String> frequentStems = new ArrayList<>();
            String topLine = "Keyword set | ";
            for (Map.Entry<String, Integer> entry : stems.entrySet()){
                String word = entry.getKey();
                Integer count = entry.getValue();
                if (count > 2){
                    frequentStems.add(word);
                    word = String.format("%10s", word);
                    topLine += word;
                    //System.out.print
                }
            }
            writer.println(topLine);

            String[] frequentStemArray = frequentStems.toArray(new String[frequentStems.size()]);
            int[][] TDM = new int[sentenceList.size()][frequentStems.size()];

            scanner = new Scanner(stemmedSentencesFile);
            Scanner sentenceScanner;
            int sentNum = 0;
            while(scanner.hasNextLine()){
                String tableEntry = String.format("Sentence %2d | ", sentNum+1);
                String sent = scanner.nextLine();
                sentenceScanner = new Scanner(sent);
                while (sentenceScanner.hasNext()){
                    String word = sentenceScanner.next();
                    for (int i = 0; i<frequentStemArray.length; i++){
                        String freq = frequentStemArray[i];
                        if (word.equals(freq)){
                            TDM[sentNum][i]++;
                        }
                        if(combinedWords.containsKey(word)){
                            if (freq.equals(combinedWords.get(word))){
                                TDM[sentNum][i]++;
                            }
                        }
                    }
                }
                for (int i = 0; i<TDM[sentNum].length; i++){
                    String entry = String.format("%10d", TDM[sentNum][i]);
                    tableEntry += entry;
                }
                sentenceScanner.close();
                sentNum++;
                writer.println(tableEntry);
            }
            scanner.close();
            writer.close();
            //int debugcatcher = 0;

            return TDM;

        } catch (FileNotFoundException e) {
            System.out.println("file not found");
        }

        return null;
    }

    public static void main(String[] args) {
        sendTDM();
    }
}
