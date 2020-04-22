package main.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class TextSimilarity {
    public static int SHINGLE_LENGTH;
    // the path to directory with email files;
    public static String EMAIL_DIR_PATH;
    // the threshold for determining similar emails according to Jaccard similarity
    public static double JACCARD_THRESHOLD;
    // the threshold that  for determining similar emails according to min-hash signatures
    public static double MINHASH_THRESHOLD;
    public static int SIGNATURE_LENGTH;

    // mapping between email's order number and the Email object
    static Map<Integer, Email> emails;

    public static void main(String[] args) {
        try{
            EMAIL_DIR_PATH = String.valueOf(args[0]);
        }
        catch (Exception e){
            EMAIL_DIR_PATH = "src\\main\\resources";
        }
        try{
            SHINGLE_LENGTH = Integer.valueOf(args[1]);
        }
        catch (Exception e){
            SHINGLE_LENGTH = 5;
        }
        try{
            JACCARD_THRESHOLD = Double.valueOf(args[2]);
        }
        catch (Exception e){
            JACCARD_THRESHOLD = 0.5;
        }
        try{
            MINHASH_THRESHOLD = Double.valueOf(args[3]);
        }
        catch (Exception e){
            MINHASH_THRESHOLD = 0.5;
        }
        try{
            SIGNATURE_LENGTH = Integer.valueOf(args[4]);
        }
        catch (Exception e){
            SIGNATURE_LENGTH = 100;
        }

        emails = readEmails();

        computeJaccardSimilarities();
        printJaccardSimilarEmails();

        buildMinHashSignatures();
        computeMinhashSimilarities();
        printMinhashSimilarEmails();

    }

    /**
     * Read all text files from the input directory and create Email object for each email.
     *
     * @return a mapping between email's name and Email object
     */
    private static Map<Integer, Email> readEmails() {
        Map<Integer, Email> emails = new HashMap<>();

        //read files and create Email objects
        File folder = new File(EMAIL_DIR_PATH);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String fileName = listOfFiles[i].getName();
                //System.out.println("File " + fileName);

                String emailContent = null;
                try {
                    emailContent = new String(Files.readAllBytes(listOfFiles[i].toPath()), "UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Email email = new Email(fileName, i, emailContent);

                emails.put(i, email);
            }
        }
        return emails;
    }

    /**
     * Compute and store Jaccard similarity for each pair of emails.
     */
    private static void computeJaccardSimilarities() {
        for (Map.Entry<Integer, Email> emailEntry1 : emails.entrySet()) {
            for (Map.Entry<Integer, Email> emailEntry2 : emails.entrySet()) {
                if (emailEntry1.getKey() < emailEntry2.getKey()) {
                    emailEntry1.getValue().computeAndSetJaccardSimilarity(emailEntry2.getValue());
                }
            }
        }
    }

    /**
     * For each email, print its similar emails according to Jaccard similarity
     */
    private static void printJaccardSimilarEmails() {
        System.out.println("Each email's similar items according to Jaccard similarity of shingle sets (threshold is " + JACCARD_THRESHOLD + "):");
        for (Email email : emails.values()) {
            System.out.print(email.fileName + ": ");
            List<String> similarEmails = email.getJaccardSimilarEmails(JACCARD_THRESHOLD);
            if (similarEmails.isEmpty()) {
                System.out.println("empty");
            } else {
                for (int i = 0; i <= similarEmails.size() - 1; i++) {
                    if (i != similarEmails.size() - 1) {
                        System.out.print(similarEmails.get(i) + ", ");
                    } else {
                        System.out.print(similarEmails.get(i));
                    }
                }
                System.out.println();
            }
        }
    }

    /**
     * For each email, build and store its min-hash signature
     */
    private static void buildMinHashSignatures() {
        // collect shingle hashes from all emails
        SortedSet<Integer> shinglesUnion = new TreeSet<>();
        for (Email email : emails.values()) {
            shinglesUnion.addAll(email.shingles);
        }

        // characteristicMatrix is the mapping between each hashed shingle's order number and the list of its emails numbers
        Map<Integer, SortedSet<Integer>> characteristicMatrix = new HashMap<>();
        // the counter is used for giving each shingle hash an order number
        int shingleCounter = 0;
        for (Integer shingleHash : shinglesUnion) {
            SortedSet<Integer> emailNumbers = new TreeSet<>();
            for (Map.Entry<Integer, Email> emailEntry : emails.entrySet()) {
                if (emailEntry.getValue().shingles.contains(shingleHash)) {
                    emailNumbers.add(emailEntry.getKey());
                }
            }
            characteristicMatrix.put(shingleCounter, emailNumbers);
            shingleCounter++;
        }

        // Get random parameters for hash functions of type (a * x + b) % number_of_shingles
        List<Integer> parametersA = new ArrayList<>();
        List<Integer> parametersB = new ArrayList<>();
        Random r = new Random();
        for (int i = 0; i < SIGNATURE_LENGTH; i++) {
            parametersA.add(r.nextInt());
            parametersB.add(r.nextInt());
        }

        // Signature matrix (transposed) maps each email number to the email's signature
        Map<Integer, List<Integer>> signatureMatrix = new HashMap<>();
        // Initialize signatureMatrix with infinity (max int value)
        for (int i = 0; i < emails.size(); i++) {
            List<Integer> initialSignature = new ArrayList<>(100);
            for (int j = 0; j < 100; j++) {
                initialSignature.add(Integer.MAX_VALUE);
            }
            signatureMatrix.put(i, initialSignature);
        }

        // Start min-hashing
        // iterate through each shingle
        for (int i : characteristicMatrix.keySet()) {
            // compute the values of hash functions (permutations)
            List<Integer> hashValues = new ArrayList<>();
            for (int hashNum = 0; hashNum < SIGNATURE_LENGTH; hashNum++) {
                Integer hashValue = Math.abs(parametersA.get(hashNum) * i + parametersB.get(hashNum)) % characteristicMatrix.size();
                hashValues.add(hashValue);
            }

            // iterate through each email
            for (int emailNum = 0; emailNum < emails.size(); emailNum++) {
                if (characteristicMatrix.get(i).contains(emailNum)) {
                    // the email contains the shingle;
                    // iterate through each hash value
                    for (int hashNum = 0; hashNum < SIGNATURE_LENGTH; hashNum++) {
                        int hashVal = hashValues.get(hashNum);
                        if (hashVal < signatureMatrix.get(emailNum).get(hashNum)) {
                            signatureMatrix.get(emailNum).set(hashNum, hashVal);
                        }
                    }
                }
            }
        }

        // Store the obtained signatures at corresponding Email objects
        for (int emailNum = 0; emailNum < emails.size(); emailNum++) {
            emails.get(emailNum).signature = signatureMatrix.get(emailNum);
        }

    }

    /**
     * Compute and store min-hash similarity for each pair of emails.
     */
    private static void computeMinhashSimilarities() {
        for (Map.Entry<Integer, Email> emailEntry1 : emails.entrySet()) {
            for (Map.Entry<Integer, Email> emailEntry2 : emails.entrySet()) {
                if (emailEntry1.getKey() < emailEntry2.getKey()) {
                    emailEntry1.getValue().computeAndSetMinhashSimilarity(emailEntry2.getValue());
                }
            }
        }
    }

    /**
     * For each email, print its similar emails according to min-hash similarity
     */
    private static void printMinhashSimilarEmails() {
        System.out.println();
        System.out.println("Each email's similar items according to min-hash similarity of signatures (threshold is " + MINHASH_THRESHOLD + "):");
        for (Email email : emails.values()) {
            System.out.print(email.fileName + ": ");
            List<String> similarEmails = email.getMinhashSimilarEmails(MINHASH_THRESHOLD);
            if (similarEmails.isEmpty()) {
                System.out.println("empty");
            } else {
                for (int i = 0; i <= similarEmails.size() - 1; i++) {
                    if (i != similarEmails.size() - 1) {
                        System.out.print(similarEmails.get(i) + ", ");
                    } else {
                        System.out.print(similarEmails.get(i));
                    }
                }
                System.out.println();
            }
        }
    }


    private static class Email {
        public String fileName;
        public Integer orderNumber;
        public String content;
        public SortedSet<Integer> shingles = new TreeSet<>();
        public List<Integer> signature = new ArrayList<>();
        // Jaccard similarity value between the current email and other emails
        public Map<Integer, Double> jaccardSimilarities = new HashMap<>();
        // Similarity of MinHash signatures between the current email and other emails
        public Map<Integer, Double> signatureSimilarities = new HashMap<>();

        Email(String fileName, int orderNumber, String content) {
            this.fileName = fileName;
            this.orderNumber = orderNumber;
            //replace each sequence of white-spaces with a single space
            this.content = content.replaceAll("\\s+", " ");

            computeAndSetShingles();
        }

        /**
         * Compute and set Jaccard similarity between the email and anotherEmail
         *
         * @param anotherEmail
         */
        public void computeAndSetJaccardSimilarity(Email anotherEmail) {
            if (!this.jaccardSimilarities.containsKey(anotherEmail.orderNumber) || !anotherEmail.jaccardSimilarities.containsKey(this.orderNumber)) {
                Set<Integer> union = new HashSet<>(this.shingles);
                union.addAll(anotherEmail.shingles);
                Set<Integer> intersections = new HashSet<>(this.shingles);
                intersections.retainAll(anotherEmail.shingles);

                Double jaccardSimilarity = ((double) intersections.size()) / union.size();

                this.jaccardSimilarities.put(anotherEmail.orderNumber, jaccardSimilarity);
                anotherEmail.jaccardSimilarities.put(this.orderNumber, jaccardSimilarity);
            }
        }

        /**
         * Compute and set min-hash similarity between the email and anotherEmail
         *
         * @param anotherEmail
         */
        public void computeAndSetMinhashSimilarity(Email anotherEmail) {
            if (!this.signatureSimilarities.containsKey(anotherEmail.orderNumber) || !anotherEmail.signatureSimilarities.containsKey(this.orderNumber)) {
                double similarPositionsCounter = 0;
                for (int i = 0; i < TextSimilarity.SIGNATURE_LENGTH; i++) {
                    if (this.signature.get(i) == anotherEmail.signature.get(i)) {
                        similarPositionsCounter++;
                    }
                }

                Double minhashSimilarity = similarPositionsCounter / TextSimilarity.SIGNATURE_LENGTH;

                this.signatureSimilarities.put(anotherEmail.orderNumber, minhashSimilarity);
                anotherEmail.signatureSimilarities.put(this.orderNumber, minhashSimilarity);
            }
        }

        /**
         * Return a list of all similar emails according to the Jaccard similarity together with the value of Jaccard similarity
         */
        public List<String> getJaccardSimilarEmails(Double threshold) {
            List<String> similarEmailNames = new LinkedList<>();
            for (Map.Entry<Integer, Double> anotherEmail : jaccardSimilarities.entrySet()) {
                if (anotherEmail.getValue() >= threshold) {
                    similarEmailNames.add(TextSimilarity.emails.get(anotherEmail.getKey()).fileName + " (" + anotherEmail.getValue() + ")");
                }
            }
            return similarEmailNames;
        }

        /**
         * Return a list of all similar emails according the min-hash signatures.
         */
        public List<String> getMinhashSimilarEmails(Double threshold) {
            List<String> similarEmailNames = new LinkedList<>();
            for (Map.Entry<Integer, Double> anotherEmail : signatureSimilarities.entrySet()) {
                if (anotherEmail.getValue() >= threshold) {
                    similarEmailNames.add(TextSimilarity.emails.get(anotherEmail.getKey()).fileName + " (" + anotherEmail.getValue() + ")");
                }
            }
            return similarEmailNames;
        }

        /**
         * Compute the set of hashed shingles for the email.
         */
        private void computeAndSetShingles() {
            for (int i = 0; i <= content.length() - 1 - TextSimilarity.SHINGLE_LENGTH; i++) {
                shingles.add(content.substring(i, i + TextSimilarity.SHINGLE_LENGTH).hashCode());
            }
        }
    }
}