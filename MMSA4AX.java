/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mmsa;

import mmsa.MatrixCreator;
/**
 *
 * @author HJS
 */
public class MMSA4AX {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        //
        MatrixCreator mc = new MatrixCreator("tags.csv");
        mc.createMatrix();
        mc.createPhotoTags();
    }
    
}
