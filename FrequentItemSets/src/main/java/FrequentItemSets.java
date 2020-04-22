import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class FrequentItemSets {
    // The mapping between the set size and the sets of frequent items of this size.
    public static Map<Integer, List<Set<Integer>>> frequentItemSets = new HashMap<>();
    // The mapping between a frequent set and its support
    public static Map<Set<Integer>, Integer> frequentSetsSupport = new HashMap<>();

    // The path to the dataset
    public static String DATASET_PATH;
    // Total number of items that can be encountered
    public static int ITEMS_NUMBER;
    // Total number of baskets
    public static int BASKETS_NUMBER;
    // Support threshold, which is a fraction of baskets that a set needs to be part of to be considered frequent
    public static float SUPPORT_THRESHOLD;
    // Confidence threshold for association rules
    public static float CONFIDENCE_THRESHOLD;

    public static void main(String[] args) throws IOException {
        try{
            DATASET_PATH = String.valueOf(args[0]);
        }
        catch (Exception e){
            DATASET_PATH = "src\\main\\resources\\T10I4D100K.dat";
        }
        try{
            ITEMS_NUMBER = Integer.valueOf(args[1]);
        }
        catch (Exception e){
            ITEMS_NUMBER = 1000;
        }
        try{
            BASKETS_NUMBER = Integer.valueOf(args[2]);
        }
        catch (Exception e){
            BASKETS_NUMBER = 100000;
        }
        try{
            SUPPORT_THRESHOLD = Float.valueOf(args[3]);
        }
        catch (Exception e){
            SUPPORT_THRESHOLD = 0.01f;
        }
        try{
            CONFIDENCE_THRESHOLD = Float.valueOf(args[4]);
        }
        catch (Exception e){
            CONFIDENCE_THRESHOLD = 0.5f;
        }

        // Run the first pass to find all frequent items
        firstPass();

       // currPass corresponds to the size of frequent item sets that are searched during the pass
        int currPass = 1;
        // Run the passes of the A-Priori algorithm until no more frequent sets are found
        while (frequentItemSets.containsKey(currPass)) {
            currPass++;
            nextPass(currPass);
        };

        // Print the frequent sets
        printFrequentSets();

        // Get and print association rules
        Map<Set<Integer>, Set<Integer>> associationRules = getAssociationRules();
        printAssociationRules(associationRules);
    }

    /**
     * Run the first pass of the A-Priori algorithm and find all frequent items.
     * @throws IOException
     */
    private static void firstPass () throws IOException {
        int[] itemCounts = new int[ITEMS_NUMBER];

        //read data from file and count occurrence of each item
        BufferedReader reader = new BufferedReader(new FileReader(DATASET_PATH));
        String basketString;
        while ((basketString = reader.readLine()) != null && !basketString.equals("")) {
            int[] basketSet = Arrays.asList(basketString.split(" ")).stream().mapToInt(Integer::parseInt).toArray();
            for (int item : basketSet) {
                itemCounts[item]++;
            }
        }
        reader.close();
        // mark frequent items
        List<Set<Integer>> frequentSingularSets = new LinkedList<>();
        for (int i = 0; i < ITEMS_NUMBER; i++) {
            if (itemCounts[i] >= BASKETS_NUMBER * SUPPORT_THRESHOLD) {
                Set<Integer> frequentSingularSet = new HashSet<>();
                frequentSingularSet.add(i);
                frequentSingularSets.add(frequentSingularSet);
                // keep the support level of the singular set
                frequentSetsSupport.put(frequentSingularSet, itemCounts[i]);
            }
        }
        // record the frequent items if they exist
        if (!frequentSingularSets.isEmpty()) {
            frequentItemSets.put(1, frequentSingularSets);
        }
    }

    /**
     * Run a pass of the A-Priori algorithm with sequential number equal to setSize.
     * Find all frequent sets of size setSize.
     * @param setSize the pass number and also the size of frequent sets that the pass seeks
     * @throws IOException
     */
    private static void nextPass (int setSize) throws IOException {
        Map<Set<Integer>, Integer> setCounts = new HashMap<>();

        //read data from file and count occurrence of each candidate set
        BufferedReader reader = new BufferedReader(new FileReader(DATASET_PATH));
        String basketString;
        while ((basketString = reader.readLine()) != null && !basketString.equals("")) {
            // get all items from the basket
            Set<Integer> basketSet = Arrays.asList(basketString.split(" ")).stream().mapToInt(Integer::parseInt)
                    .boxed().collect(Collectors.toSet());
            // exclude from basketSet items that are not frequent
            Set<Integer> frequentItems = frequentItemSets.get(1).stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
            basketSet.retainAll(frequentItems);

            // An additional candidate restriction for sets of sizes 3 and larger
            if (setSize > 2) {
                // In order to be the candidate, the item has to appear in at least (setSize - 1) sets of size (setSize - 1),
                // while other items of these sets must also belong to the current basket.
                // relevantFrequentSets are frequent sets of size (setSize - 1) that contain only values from the current basket.
                List<Set<Integer>> relevantFrequentSets = frequentItemSets.get(setSize - 1).stream()
                        .filter(relevantFrequentSet -> basketSet.containsAll(relevantFrequentSet)).collect(Collectors.toList());
                Set<Integer> itemsForRemoval = new HashSet<>();
                for (Integer item : basketSet) {
                    long itemCounter = relevantFrequentSets.stream().filter(relevantFrequentSet -> relevantFrequentSet.contains(item)).count();
                    if (itemCounter < setSize - 1) {
                        // There are not enough occurrences of the item in the current basket, so it should be removed
                        itemsForRemoval.add(item);
                    }
                }
                basketSet.removeAll(itemsForRemoval);
            }

            if (basketSet.size() >= setSize) {
                // generate candidate sets using candidate items from the basket
                Set<Set<Integer>> candidateSets = getSubsetsOfSize(basketSet, setSize);
                //increment the counter for each candidate set
                for (Set<Integer> candidateSet : candidateSets) {
                    if (!setCounts.containsKey(candidateSet)) {
                        setCounts.put(candidateSet, 1);
                    }
                    else {
                        int prevCount = setCounts.get(candidateSet);
                        setCounts.replace(candidateSet, prevCount + 1);
                    }

                }
            }
        }
        reader.close();
        // mark frequent sets
        List<Set<Integer>> frequentSets = new LinkedList<>();
        for (Set<Integer> candidateSet : setCounts.keySet()) {
            int setCount = setCounts.get(candidateSet);
            if (setCount >= BASKETS_NUMBER * SUPPORT_THRESHOLD) {
                frequentSets.add(candidateSet);
                // keep the support level of the set
                frequentSetsSupport.put(candidateSet, setCount);
            }
        }
        // record the frequent sets if they exist
        if (!frequentSets.isEmpty()) {
            frequentItemSets.put(setSize, frequentSets);
        }
    }

    /**
     * Get all association rules with a confidence level of at least CONFIDENCE_THRESHOLD
     */
    private static Map<Set<Integer>, Set<Integer>> getAssociationRules () {
        Map<Set<Integer>, Set<Integer>> associationRules = new HashMap<>();
        // Try to extract an association rule from each frequent set
        for (Set<Integer> frequentSet : frequentSetsSupport.keySet()) {
            // A frequent set should be of at least size 2 in order to produce an association rule
            if (frequentSet.size() > 1) {
                for (int k = 1; k < frequentSet.size(); k++) {
                    // Try to construct candidate rules by putting subsets of size k to the left part of the rule and the sets of remaining items to the right side
                    Set<Set<Integer>> subsetsOfSizeK = getSubsetsOfSize(frequentSet, k);
                    for (Set<Integer> subsetOfSizeK : subsetsOfSizeK) {
                        // Check if the candidate rule passes the confidence threshold
                        float ruleConfidence = Float.valueOf(frequentSetsSupport.get(frequentSet)) / frequentSetsSupport.get(subsetOfSizeK);
                        if (ruleConfidence >= CONFIDENCE_THRESHOLD) {
                            // the association rule is identified as a confident one
                            Set<Integer> rightPart = new HashSet<>(frequentSet);
                            rightPart.removeAll(subsetOfSizeK);
                            associationRules.put(subsetOfSizeK, rightPart);
                        }
                    }
                }
            }
        }
        return associationRules;
    }

    /**
     * Returns all possible combinations of items of the given size
     * @param items the set of provided items
     * @param size the size subsets
     */
    private static Set<Set<Integer>> getSubsetsOfSize(Set<Integer> items, int size) {
        if (size == 1) {
            Set<Set<Integer>> singularSets = new HashSet<>();
            for (Integer item : items) {
               Set<Integer> singularSet = new HashSet<>();
               singularSet.add(item);
               singularSets.add(singularSet);
            }
            return singularSets;
        }
        Set<Set<Integer>> combinations = new HashSet<>();
        Set<Integer> reducedSet = new HashSet<>(items);
        for (Integer item : items) {
            reducedSet.remove(item);
            Set<Set<Integer>> combinationsOfSmallerSize = getSubsetsOfSize(reducedSet, size - 1);
            for (Set<Integer> combination : combinationsOfSmallerSize) {
                combination.add(item);
                combinations.add(combination);
            }
        }
        return combinations;
    }

    /**
     * Print all frequent set that appear in frequentItemSets,
     * as well as the sets' support values from frequentSetsSupport.
     */
    private static void printFrequentSets () {
        System.out.println("Frequent item sets of all sizes and their support values (support threshold = " + SUPPORT_THRESHOLD + "):");
        for (Map.Entry<Integer, List<Set<Integer>>> frequentItemSetsEntry : frequentItemSets.entrySet()) {
            System.out.print(frequentItemSetsEntry.getKey() + ": ");
            for (Set<Integer> frequentSet : frequentItemSetsEntry.getValue()) {
                System.out.print("{" + String.join(",", frequentSet.stream().map(i -> i + "").collect(Collectors.toList())) + "}");
                System.out.print(" ("+ frequentSetsSupport.get(frequentSet) +"), ");
            }
            System.out.println();
        }
    }

    /**
     * Print association rules
     * @param associationRules contains association rules to be printed
     */
    private static void printAssociationRules (Map<Set<Integer>, Set<Integer>> associationRules){
        System.out.println("\nAssociation rules and their confidence values (confidence threshold = " + CONFIDENCE_THRESHOLD + "):");
        for (Map.Entry<Set<Integer>, Set<Integer>> associationRule : associationRules.entrySet()) {
            for(Integer leftItem : associationRule.getKey()){
                System.out.print(leftItem + " ");
            }
            System.out.print("-> ");
            for(Integer rightItem : associationRule.getValue()){
                System.out.print(rightItem + " ");
            }
            // print the rule confidence
            Set<Integer> ruleItems = new HashSet<>(associationRule.getKey());
            ruleItems.addAll(associationRule.getValue());
            System.out.print(" (" + Float.valueOf(frequentSetsSupport.get(ruleItems)) / frequentSetsSupport.get(associationRule.getKey()) + ")\n");
        }
    }
}

