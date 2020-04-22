import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

public class MiningDataStreams {
    // The path to a text file that contains the graph
    public static String FILE_PATH;
    // Parameter b for HyperLogLog; 2^b is the number of used registers
    public static int HLL_B;
    // The path to the output csv file where the metrics are written
    public static String OUTPUT_FILE_PATH;

    public static void main(String[] args) throws Exception {
        try{
            FILE_PATH = String.valueOf(args[0]);
        }
        catch (Exception e){
            FILE_PATH = "src\\main\\resources\\com-amazon.ungraph.txt";
        }
        try{
            HLL_B = Integer.valueOf(args[1]);
        }
        catch (Exception e){
            HLL_B = 11;
        }
        try{
            OUTPUT_FILE_PATH = String.valueOf(args[2]);
        }
        catch (Exception e){
            OUTPUT_FILE_PATH = "src\\main\\resources\\Node centralities.csv";;
        }

        // Part 1 - Count the approximate number of nodes
        int approxNodesNum = countDistinctNodes();
        System.out.println("The approximate number of distinct nodes is " + approxNodesNum);

        // Part 2 - Calculate and print geometric centralities
        Set<Node> nodesSet = readNodesIntoSet();
        HyperBall hb = new HyperBall(OUTPUT_FILE_PATH, nodesSet);
        hb.calculateGeometricCentralities(HLL_B);
        System.out.println();

    }

    /**
     * Read the text file and count an approximate number of distinct nodes using HyperLogLog algorithm
     * @return the approximate number of distinct nodes
     * @throws Exception
     */
    public static int countDistinctNodes() throws Exception {
        System.out.println("Counting the approximate number of distinct nodes at the text file using "
                            + (int) Math.pow(2, HLL_B) + " registers");
        HyperLogLogCounter nodesCounter = new HyperLogLogCounter(HLL_B);

        //Read the edges line by line
        BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH));
        String edgeRow;
        while ((edgeRow = reader.readLine()) != null) {
            if (!edgeRow.startsWith("#")){
                String[] nodesStrings = edgeRow.split("\\s+");
                int nodeFrom = Integer.valueOf(nodesStrings[0]);
                int nodeTo = Integer.valueOf(nodesStrings[1]);
                // Add both nodes to the counter
                nodesCounter.add(nodeFrom);
                nodesCounter.add(nodeTo);
            }
        }
        reader.close();
        // Estimate the number of distinct nodes using the counter
        return nodesCounter.size();
    }

    /**
     * Read the text file and return the set of Node objects.
     * Each Node object represents a distinct node from the file and all its edges.
     * @return
     */
    public static Set<Node> readNodesIntoSet() throws Exception {
        System.out.println("Loading the nodes from the text file into main memory");

        // Mapping between the node value and its corresponding Node object
        Map<Integer, Node> nodeMapping = new HashMap<>();

        //Read the edges line by line
        BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH));
        String edgeRow;
        while ((edgeRow = reader.readLine()) != null) {
            if (!edgeRow.startsWith("#")){
                String[] nodesStrings = edgeRow.split("\\s+");
                int nodeFromValue = Integer.valueOf(nodesStrings[0]);
                int nodeToValue = Integer.valueOf(nodesStrings[1]);

                Node nodeFrom, nodeTo;
                if (nodeMapping.containsKey(nodeFromValue)) {
                    nodeFrom = nodeMapping.get(nodeFromValue);
                }
                else {
                    nodeFrom = new Node(nodeFromValue);
                }
                if (nodeMapping.containsKey(nodeToValue)) {
                    nodeTo = nodeMapping.get(nodeToValue);
                }
                else {
                    nodeTo = new Node(nodeToValue);
                }

                // Add the nodes to each other's neighbors set
                nodeFrom.neighbors.add(nodeTo);
                nodeTo.neighbors.add(nodeFrom);

                // Put the both Node objects to the mapping
                nodeMapping.put(nodeFromValue, nodeFrom);
                nodeMapping.put(nodeToValue, nodeTo);

            }
        }
        reader.close();
        // Now the nodeMapping contains all necessary Node objects as values
        return nodeMapping.values().stream().collect(Collectors.toSet());
    }
}


class HyperLogLogCounter {
    // The constant used for fixing the bias when computing the size() method
    // ALPHA -> 0.72134 when the number of registers -> infinity
    static final double ALPHA = 0.72134;

    // The array of registers
    byte[] c;

    HyperLogLogCounter(int b){
        // Create an array of registers of length 2^b and initialized with 0
        int p = (int) Math.pow(2, b);
        c = new byte[p];
    }

    /**
     * Copy constructor
     * Copies the registers array of hllCnt to the registers array of the new counter
     * @param hllCnt
     */
    HyperLogLogCounter(HyperLogLogCounter hllCnt){
        this.c = hllCnt.c.clone();
    }

    /**
     * Add the integer node value to the counter
     * @param node value to be added to the counter
     * @throws Exception
     */
    public void add(int node) throws Exception {
        // Use SHA-1 hash function to obtain 128 random bits for each node
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(ByteBuffer.allocate(4).putInt(node).array());
        byte[] bytes = md.digest();

        // Take first 4 bytes for determining the register number
        byte[] bytes1 = Arrays.copyOfRange(bytes, 0, 4);
        // Take next 8 bytes as a value of the hash value used for counting trailing zeros
        byte[] bytes2 = Arrays.copyOfRange(bytes, 4, 12);

        // Determine the assigned register number
        int regNum = ByteBuffer.wrap(bytes1).getInt() & (c.length - 1);

        // Calculate the hash value of the node and check the number of trailing zeros
        long hashValue = ByteBuffer.wrap(bytes2).getLong();
        byte trailingZerosCnt = (byte) Long.numberOfTrailingZeros(hashValue);
        // Increase the number of trailing zeros with 1 for the later use
        trailingZerosCnt ++;

        // Update the register if the new trailingZerosCnt is greater than the current one.
        if(trailingZerosCnt > c[regNum]){
            c[regNum] = trailingZerosCnt;
        }
    }

    /**
     * Calculate the harmonic mean of the registers and return the estimated value of the counter
     * @return the estimated value of the counter
     */
    public int size(){
        double zDivisor = 0;
        for(int i = 0; i < c.length; i++){
            zDivisor += Math.pow(2, -1 * c[i]);
        }
        double z = 1 / zDivisor;
        int estimator = (int) (ALPHA * c.length * c.length * z);
        return estimator;
    }
}


class HyperBall {
    String outputFilePath;
    Set<Node> nodesSet;

    public HyperBall(String outputFilePath, Set<Node> nodesSet){
        this.outputFilePath = outputFilePath;
        this.nodesSet = nodesSet;
    }

    /**
     * Implement the HyperBall algorithm.
     * Calculate all necessary metrics for estimating the geometric centralities:
     *  - the sum of the distances to each node;
     *  - the sum of the reciprocals of the distances to each node;
     *  - the size of the coreachable set of each node.
     *  The calculated metrics for each node are stored at its Node object.
     */
    public void calculateGeometricCentralities(int b) throws Exception {
        System.out.println("Starting the HyperBall algorithm");

        // Initialize a HyperLogLog counter for each node
        for (Node v : nodesSet) {
            v.hllCounter = new HyperLogLogCounter(b);
            v.hllCounter.add(v.value);
        }
        // Current ball size that each of the node's counters represent
        int t = 0;
        // Loop through all nodes several times until no counter changes its value
        boolean ctrValueChanged = false;
        do {
            // Update map (in-memory implementation of the update map)
            Map<Node, HyperLogLogCounter> updateMap = new HashMap<>();

            System.out.println("Calculating ball sizes of radius: " + (t + 1));
            for (Node v : nodesSet) {
                // Counter associated with Node v; contains the approximation of |B(v, t)|
                HyperLogLogCounter cV = v.hllCounter;
                // Counter that will contain the approximation of |B(v, t + 1)| at the end of the node
                HyperLogLogCounter a = new HyperLogLogCounter(cV);

                // Loop through all neighbors of V
                for (Node w : v.neighbors) {
                    union(a, w.hllCounter);
                }

                // Write the (Node, new counter) pair to the update map if the new counter differs from the old one
                if (!Arrays.equals(cV.c, a.c)) {
                    updateMap.put(v, a);
                }

                // Approximation of the number of nodes at distance t + 1 : |B(v, t + 1)| - |B(v, t)|
                int nodesAtDistanceTPlusOne = a.size() - cV.size();

                // Save the updated values of currSumOfDistances and currSumOfRecDistances into Node v.
                v.currSumOfDistances += (t + 1) * nodesAtDistanceTPlusOne;
                v.currSumOfRecDistances += Double.valueOf(nodesAtDistanceTPlusOne) / (t + 1);
            }

            if(updateMap.isEmpty()){
                // No changes during the last iteration, so the algorithm stops
                ctrValueChanged = false;
            }
            else {
                // There were some changes, so the algorithm goes on
                ctrValueChanged = true;
                // Read the pairs (v, a) form the updateMap and update the nodes' counters
                for (Map.Entry<Node, HyperLogLogCounter> nodeCounterPair : updateMap.entrySet()) {
                    nodeCounterPair.getKey().hllCounter = nodeCounterPair.getValue();
                }
            }
            t ++;
        } while (ctrValueChanged);

        printCentralities();
    }

    /**
     * Print node centralities metrics to the output file. The metrics are:
     *  - Closeness centrality;
     *  - Lin's centrality;
     *  - Harmonic centrality
     */
    private void printCentralities() throws Exception {
        System.out.println("Writing the centrality metrics to the output file");
        FileWriter csvWriter = new FileWriter(outputFilePath);
        csvWriter.append("Node,Closeness centrality,Lin's centrality, Harmonic centrality\n");
        for (Node node : nodesSet) {
            double closenessCentr = Double.valueOf(1) / node.currSumOfDistances;
            double LinsCentr = Math.pow(node.hllCounter.size(), 2) / node.currSumOfDistances;
            double harmonicCentr = node.currSumOfRecDistances;
            csvWriter.append(node.value + "," + closenessCentr + ","
                                              + String.format("%.2f", LinsCentr) + ","
                                              + String.format("%.2f", harmonicCentr));
            csvWriter.append("\n");
        }
        csvWriter.close();
        System.out.println("The centrality metrics have been written to the file successfully");
    }

    /**
     * Update counterM so that it represents the sum of counterM and counterN.
     * This is done by maximizing the values of two counters.
     * @param counterM
     * @param counterN
     */
    public void union(HyperLogLogCounter counterM, HyperLogLogCounter counterN) {
        for (int i = 0; i < counterM.c.length; i++){
            counterM.c[i] = (byte) Math.max(counterM.c[i], counterN.c[i]);
        }
    }
}

class Node {
    // The integer value of a node
    public int value;
    // The set of all node's neighbors
    public Set<Node> neighbors;

    // Variables Required for HyperBall centrality metrics calculations:

    // The counter associated with the Node
    // Used for Lin's centrality calculations
    public HyperLogLogCounter hllCounter;
    // The accumulated sum of all distances to the node : SUM_y d(y, x)
    // Used for Closeness centrality and Lin's centrality calculations
    public int currSumOfDistances;
    // The accumulated sum of reciprocals of all distances to the node : SUM_(y!=x) 1/d(y, x)
    // Used for harmonic centrality calculations
    public double currSumOfRecDistances;

    public Node(int value){
        this.value = value;
        this.neighbors = new HashSet<>();
        this.currSumOfDistances = 0;
        this.currSumOfRecDistances = 0;
    }
}
