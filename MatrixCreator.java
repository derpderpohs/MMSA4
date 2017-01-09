/*
* This class formats the matrix for the the application
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MatrixCreator {

    private Map<String, Map<String, Double>> matrix; //outer string references x axis values
    private Map<String, Double> yAxis, idfList; //each item in the y axis; this map exists for every matrix element.
    private Map<String, List<String>> photoTags; //groups tags that belong to the same picture
    private final String fileName; 
    private final String splitter = ",";
    public final double totalImg = 10000; //we already know the total number of images
    private String line = "";
    private Map<String, Double> listOfTerms; //used to calculate TF-IDF 

    public MatrixCreator(String filename) {
        fileName = filename;
    }

    public static void main(String[] args) {
        MatrixCreator mc = new MatrixCreator("tags.csv");
        mc.createMatrix();
        mc.createPhotoTags();
    }

    //read tags.csv
    public Map<String, Map<String, Double>> createMatrix() {
        listOfTerms = new HashMap<>();
        matrix = new HashMap<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                String[] csvTerms = line.split(splitter);
                String key = csvTerms[0];
                //add terms into a list to iterate through for xentry later
                listOfTerms.put(csvTerms[0], Double.parseDouble(csvTerms[1]));
                //store list of tags in to an arraylist

                matrix.put(key, yAxis);
                //put yaxis values into inner hashmap
            }
            br.close();
        } catch (Exception e) {
        }
        createPhotoTags();
        return populateMatrix();
    }

    //create Yaxis
    public Map<String, Map<String, Double>> populateMatrix() {
        //initialise the y axis
        yAxis = new HashMap<>();
        for (Map.Entry<String, Double> s : listOfTerms.entrySet()) {
            //initialise each value in the y axis to -1 indicating no corelation
            yAxis.put(s.getKey(), 0.0);

        }

        for (Map.Entry<String, Map<String, Double>> s : matrix.entrySet()) {
            Map<String, Double> tmp = new HashMap();
            tmp.putAll(yAxis);
            //replace the matrix y axis with these uninitialised values
            matrix.put(s.getKey(), tmp);
            ;
        }
        setCorelation();

        printMatrix();
        return matrix;
    }

    //Read photo_tags.csv. this method stores the data in the csv to photoTags;
    //the purpose is to group the disparate data together for correlation so it would be easier
    //to match afterword.
    public Map<String, List<String>> createPhotoTags() {
        photoTags = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("photos_tags.csv"));
            while ((line = br.readLine()) != null) {
                String[] terms = line.split(splitter);
                List<String> cur = photoTags.get(terms[0]);
                if (cur == null) {
                    cur = new ArrayList<>();
                }
                cur.add(terms[1]);
                photoTags.put(terms[0], cur);
            }
            br.close();
        } catch (Exception e) {
        }

        return photoTags;
    }

    //read photoTags, then add 1 for each corelation. this method adds corelation
    //w/o computing TF-IDF weights
    private void setCorelation() {
        //for each photo
        for (Map.Entry<String, List<String>> s : photoTags.entrySet()) {
            //in each list
            String first, second;
            List<String> terms = s.getValue();
            //for each list of terms in photo
            for (int i = 0; i < terms.size(); i++) {

                if (terms.size() <= i + 1) {
                    //check if second term exists. if not, we have reached the end
                    continue;
                }
                for (int j = i + 1; j < terms.size(); j++) {
                    first = terms.get(i);
                    second = terms.get(j);

                    //add one to the relationship for both sides both ways. i.e a & b, b& a
                    //for a & b
                    Map<String, Double> xAxis = matrix.get(first);
                    double firstVal = xAxis.get(second) == -1 ? 1 : xAxis.get(second) + 1;
                    xAxis.put(second, firstVal);
                    //for b & a
                    Map<String, Double> yAxis = matrix.get(second);
                    double secondVal = yAxis.get(first) == -1 ? 1 : yAxis.get(first) + 1;
                    yAxis.put(first, secondVal);

                }

            }

        }

    }

    //this method prints out the top 5 tags and writes it to a csv file.
    public void printMatrix() {
        String text = ",";
        try {
            FileWriter fw = new FileWriter("matrix.csv");

            ArrayList<String> cat = new ArrayList();
            for (Map.Entry<String, Double> m : listOfTerms.entrySet()) {
                //print the original values first
                text += m.getKey() + ",";
                cat.add(m.getKey());
            }
            fw.append(text + "\n");
            int count = 0;
            double totalCatImg = 0;
            for (Map.Entry<String, Map<String, Double>> s : matrix.entrySet()) {
                text = cat.get(count) + ",";
                totalCatImg = 0;
                for (int k = 0; k < cat.size(); k++) {
                    //iterate and add the category results to the string
                    double cur = s.getValue().get(cat.get(k));
                    text += cur + ",";
                    totalCatImg += cur;
                }
                text += "\n";
                fw.append(text);
                count++;
            }
            recommendTags(matrix);
            System.out.println("--------------------------------------------");
            calIDF();
            generateIDFMatrix();
            recommendTags(matrix);

        } catch (IOException ex) {
            Logger.getLogger(MatrixCreator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    //this method computes the top tags for the 3 required tags in the AX for both ways.
    public void recommendTags(Map<String, Map<String, Double>> m) {
        ArrayList<String> rTags = new ArrayList();

        rTags.add("water");
        rTags.add("people");
        rTags.add("london");

        //createa a custom comparator because the default cannot access the double value in our entry.
        Comparator<Map.Entry<String, Double>> comp = (Map.Entry<String, Double> a, Map.Entry<String, Double> b) -> {
            return b.getValue().compareTo(a.getValue());
        };

        for (int i = 0; i < rTags.size(); i++) {
            String currentTag = rTags.get(i);
            List<Map.Entry<String, Double>> l = new ArrayList<>(m.get(currentTag).entrySet());
            l.sort(comp);
            System.out.println("current tag is " + currentTag);
            for (int k = 0; k < 5; k++) {
                System.out.println("top " + (k + 1) + " is " + l.get(k));
            }
            System.out.println("");
        }
    }

    //calculates IDF of a tag
    public void calIDF() {
        idfList = new HashMap<>();
        for (Map.Entry<String, Double> m : listOfTerms.entrySet()) {
            double val = Math.log10(totalImg / m.getValue());
            idfList.put(m.getKey(), val);
        }
    }

    //this method transforms the weight of the original cooccurance matrix to its IDF form.
    public void generateIDFMatrix() {
        for (Map.Entry<String, Map<String, Double>> m : matrix.entrySet()) {
            for (Map.Entry<String, Double> ma : m.getValue().entrySet()) {
                ma.setValue(idfList.get(ma.getKey()) * ma.getValue());
            }
        }
    }
}
