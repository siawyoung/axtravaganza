package com.example.testlocation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.KeyPoint;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

// NOTICE: when generate javadoc, add '-J-Duser.language=en' in VM option
/**
 * @author Zhou Yiren
 *
 *
 */
public class ComparePicture extends ActionBarActivity {
    // load the .so file for SIFT model
    static
    {
        try
        {
            /**
             * Load OpenCV library and SIFT matching code from nonfree module
             */
            // Load necessary libraries.
            System.loadLibrary("opencv_java");
            System.loadLibrary("nonfree");
            System.loadLibrary("nonfree_jni");
        }
        catch( UnsatisfiedLinkError e )
        {
            System.err.println("Native code library failed to load.\n" + e);
        }
    }

    // private variables
    private ImageView imageView1;
    private ImageView imageView2;
    private TextView textView;

    private Bitmap matchImageBitmap;
    // SIFT keypoint and descriptor
    private MatOfKeyPoint keypoints_query = new MatOfKeyPoint();
    private Mat descriptors_query = new Mat();
    private MatOfKeyPoint keypoints_compare = new MatOfKeyPoint();
    private Mat descriptors_compare = new Mat();

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String FILE_TYPE = ".jpg";
    private static final String FILE_TYPE_DATA = ".xml";
    // number of dataset image
    private int NumCompare = 11;

    private double thresh = 1.5;
    private int maxIteration = 50;

    // extract the needed coordinate
    /**
     * Method to extract the coordinates of matched points
     * @param kp1 SIFT keypoint in the first image
     * @param kp2 SIFT keypoint in the second image
     * @param matches match index of 2 images
     * @param arraylist1 coordinates of matched points in the first image
     * @param arraylist2 coordinates of matched points in the second image
     */
    void extractMatchCoordinate(MatOfKeyPoint kp1, MatOfKeyPoint kp2, MatOfDMatch matches, ArrayList<Point> arraylist1, ArrayList<Point> arraylist2){
        List<KeyPoint> list1 = kp1.toList();
        List<KeyPoint> list2 = kp2.toList();
        List<DMatch> match = matches.toList();

        // get the match coordinate
        for (int i = 0; i < match.size(); i++){
            DMatch match_idx = match.get(i);
            arraylist1.add(list1.get(match_idx.queryIdx).pt);
            arraylist2.add(list2.get(match_idx.trainIdx).pt);
        }
    }

    // add all the 2-D array(with same size) in the list to one single 2-D array
    private double[][] get_whole_matrix(List<double[][]> list){
        double[][] array = new double[][]{};
        List<double[]> list_temp = new ArrayList<double[]>();
        for (int i = 0; i < list.size(); i++){
            list_temp.addAll(Arrays.<double[]> asList(list.get(i)));
        }
        return list_temp.toArray(array);
    }

    // convert 1-D array to 2-D array
    private double[][] oneDtwoD(double[] a, int m, int n){
        if (a.length != m*n){
            throw new IllegalArgumentException("Number of 1-D array and 2-D array elements don't match!");
        }
        double[][] A = new double[m][n];
        for (int i = 0; i < m; i++){
            for (int j = 0; j < n; j++){
                A[i][j] = a[j*n + i%n];
            }
        }
        return A;
    }

    // get matrix product
    private double[][] matrix_product(double[][] A, double[][] B) {

        int aRows = A.length;
        int aColumns = A[0].length;
        int bRows = B.length;
        int bColumns = B[0].length;

        if (aColumns != bRows) {
            throw new IllegalArgumentException("A:Rows: " + aColumns + " did not match B:Columns " + bRows + ".");
        }
        double[][] C = new double[aRows][bColumns];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                C[i][j] = 0.00000;
            }
        }
        for (int i = 0; i < aRows; i++) { // aRow
            for (int j = 0; j < bColumns; j++) { // bColumn
                for (int k = 0; k < aColumns; k++) { // aColumn
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return C;
    }

    // get homography score
    /**
     * Method to get homography score based on difference value;
     * if difference is less than threshold, consider it's match
     * @param x1 vectors for first image
     * @param x2 vectors for second image
     * @param threshold value for determine whether match or not
     * @return number of match
     */
    int score_homography(double[][] x1, double[][] x2, int threshold){
        return 0;
    }


    // new function added on Nov 23, 2014
    // get transformation matrix
    /**
     * Method to estimate the transformation matrix
     * @param num_choose number of chosen points
     * @param X1_chosen chosen points from first image
     * @param X2_chosen chosen points from second image
     * @return H transformation matrix
     */
    private double[][] estimateGeoTransMat(int num_choose, double[][] X1_chosen, double[][] X2_chosen) // return H
    {
        // get variables
        double[][] x1 = new double[1][3];
        double[][] x2 = new double[3][3];
        // initialize the list_A
        List<double[][]> list_A = new ArrayList<double[][]>();

        // get the random selected point pairs
        for (int j = 0; j < num_choose; j++){
            // x1
            x1[0][0] = X1_chosen[0][j];	x1[0][1] = X1_chosen[1][j];	x1[0][2] = X1_chosen[2][j];
            // skew symmetric matrix of x2
            x2[0][0] = 0;	x2[0][1] = -X2_chosen[2][j];	x2[0][2] = X2_chosen[1][j];
            x2[1][0] = X2_chosen[2][j];	x2[1][1] = 0;	x2[1][2] = -X2_chosen[0][j];
            x2[2][0] = -X2_chosen[1][j];	x2[2][1] = X2_chosen[0][j];	x2[2][2] = 0;
            // calculate matrix list_A{j}
            list_A.add(KroneckerOperation.product(x1, x2));
        }

        // get matrix A
        double[][] A = get_whole_matrix(list_A);
        // convert A to matrix
        SimpleMatrix matA = new SimpleMatrix(A);
        // get SVD of A
        SimpleSVD s =matA.svd();
        // get matrix V
        SimpleMatrix V = s.getV();
        // convert back to array
        double[] v_1D = V.getMatrix().getData();
        double[][] v = oneDtwoD(v_1D, 9, 9);

        // only choose the last row
        double[] v9 = new double[9];
        for (int j = 0; j < 9; j++){
            v9[j] = v[8][j];
        }

        // get matrix H
        double[][] H = oneDtwoD(v9, 3, 3);
        return H;
    }

    // using RANSAC to compare the two matched keypoints
    /**
     * Method for to apply RANSAC
     * @param AL1 coordinate list of matched points in the first image
     * @param AL2 coordinate list of matched points in the second image
     * @param maxTime number of iteration times
     * @return number of match after RANSAC
     */
    int RANSAC_match(ArrayList<Point> AL1, ArrayList<Point> AL2, int maxTime){

        ArrayList<double[]> AL1array = convertPointsToArrays(AL1);
        ArrayList<double[]> AL2array = convertPointsToArrays(AL2);

        int numberOfChosenPoints = 0;
        int maxScore = 0;

        double[][] geoTransMat = null;

        for(int i = 0; i < maxTime; i++) {
            int score = 0;
            int[] chosenPoints = randomInts(AL1.size());

            ArrayList<double[]> chosenAL1 = new ArrayList<double[]>();
            ArrayList<double[]> chosenAL2 = new ArrayList<double[]>();

            for (int cP : chosenPoints) {
                chosenAL1.add(AL1array.get(cP));
                chosenAL2.add(AL2array.get(cP));
            }

            double[][] AL1Matrix = convertListToMatrix(chosenAL1);
            double[][] AL2Matrix = convertListToMatrix(chosenAL2);

            geoTransMat = estimateGeoTransMat(chosenPoints.length, AL1Matrix, AL2Matrix);

            for (int j = 0; j < chosenAL1.size(); j++) {
                if (Arrays.equals(matrix_product(geoTransMat,
                        oneDtwoD(chosenAL1.get(i), chosenAL1.get(i).length, 1))
                        , oneDtwoD(chosenAL2.get(i), chosenAL2.get(i).length, 1))) {
                    score++;

                }
            }

            if (score > maxScore) {
                maxScore = score;
            }
        }
        return maxScore;
    }

    /**
     * Converts an ArrayList of Points to matrices
     * @param AL
     * @return
     */
    ArrayList<double[]> convertPointsToArrays(ArrayList<Point> AL){
        ArrayList<double[]> matrixList = new ArrayList<double[]>();
        for (Point p : AL){
            double[] newMat = {p.x, p.y, 1};
            matrixList.add(newMat);
        }
        return matrixList;
    }

    double[][] convertListToMatrix(ArrayList<double[]> pointList){
        double[][] pointMatrix = new double[pointList.size()][3];
        for(int i = 0; i < pointList.size(); i++){
            pointMatrix[0][i] = pointList.get(i)[0];
            pointMatrix[1][i] = pointList.get(i)[1];
            pointMatrix[2][i] = pointList.get(i)[2];
        }
        return pointMatrix;
    }
    int[] randomInts (int arrayLength){
        int[] ints = new int[new Random().nextInt(arrayLength / 2) + arrayLength / 2];

        for(int i = 0; i < ints.length; i++){
            ints[i] = new Random().nextInt(arrayLength);
        }

        return ints;
    }


    /* (non-Javadoc)
     * @see android.support.v7.app.ActionBarActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("TestLog","ComparePicture: onCreate");



        setContentView(R.layout.activity_compare_picture);

        imageView1 = (ImageView)findViewById(R.id.compareImageView1);
        imageView2 = (ImageView)findViewById(R.id.compareImageView2);
        textView = (TextView)findViewById(R.id.CompareResult);

        // get query image SIFT descriptors
        String queryPath = ((GlobalVariables)getApplication()).getPath();
        String queryName = ((GlobalVariables)getApplication()).getNameSmall();

        Log.d("TestLog","ComparePicture: " + queryPath);
        Log.d("TestLog","ComparePicture: " + queryName);


        File queryFile = new File(queryPath, queryName);

        // get new mat for match
        MatOfDMatch matches = new MatOfDMatch();

        Log.i("!!!!!!!", queryFile.getAbsolutePath());

        // compute keypoints and descriptors from the input query image
        getSIFT(queryFile.getAbsolutePath(), keypoints_query.getNativeObjAddr(), descriptors_query.getNativeObjAddr());

        // get SIFT descriptors from image list, and get matching score based on RANSAC
        String comparePath = queryPath + "/land_mark";

        // string to show the compare result
        String result_text = "Compare result:\n";

        // ONLY COMPARE WITH IMAGE 7, for testing
        int i = 7;	// compare with database image 7
        int numberOfCorrectCorrespondence = 0;


        // get new arraylist to store the matched coordinates
        ArrayList<Point> coordinate_query = new ArrayList<Point>();
        ArrayList<Point> coordinate_compare = new ArrayList<Point>();

        // get single image descriptors
        // by directly loading the data file
        String compareNameData = "image_data" + String.valueOf(i) + FILE_TYPE_DATA;
        File compareFileData = new File(comparePath, compareNameData);
        Log.i("Compare data name:", compareFileData.getAbsolutePath());
        // load keypoints and descriptors for a database image
        getKeypointAndDescriptor(compareFileData.getAbsolutePath(), keypoints_compare.getNativeObjAddr(), descriptors_compare.getNativeObjAddr());
        // match the two descriptors

        // JNI implementation of getMatch
        //public static native void getMATCH(long addrDescriptor1, long addrDescriptor2, long addrMatch);
        getMATCH(descriptors_query.getNativeObjAddr(), descriptors_compare.getNativeObjAddr(), matches.getNativeObjAddr());
        Log.d("descriptorsQuery", descriptors_query.toString());
        Log.d("descriptorsCompare", descriptors_compare.toString());
        Log.d("matches", matches.toString());

//        getMatchJava(descriptors_query, descriptors_compare, matches);

        // extract the needed coordinate
        extractMatchCoordinate(keypoints_query, keypoints_compare, matches, coordinate_query, coordinate_compare);
        // using RANSAC to compare the two matched keypoints
        numberOfCorrectCorrespondence = RANSAC_match(coordinate_query, coordinate_compare, maxIteration);

        Log.d("TestLog", "numberOfCorrectCorrespondence: " + numberOfCorrectCorrespondence);
        Log.d("TestLog", "query: " + coordinate_query.size());
        Log.d("TestLog", "database: " + coordinate_compare.size());

        // add the result to result text
        result_text = result_text + "Image" + Integer.toString(i) + ":	" + Integer.toString(numberOfCorrectCorrespondence) + "/" + Integer.toString(coordinate_query.size()) + "\n";


        // show the two images
        final Options options = new Options();
        // show query image
        matchImageBitmap = BitmapFactory.decodeFile(queryFile.getAbsolutePath(), options);
        imageView1.setImageBitmap(matchImageBitmap);
        imageView1.setVisibility(View.VISIBLE);
        // show match image
        String matchName = String.valueOf(i) + FILE_TYPE;
        File matchFile = new File(comparePath, matchName);
        matchImageBitmap = BitmapFactory.decodeFile(matchFile.getAbsolutePath(), options);
        imageView2.setImageBitmap(matchImageBitmap);
        imageView2.setVisibility(View.VISIBLE);

        // show the match result for all dataset images
        textView.setText(result_text);
    }

    /**
     * Takes addressDescriptors of OpenCV Mat data types, and mutates MatOfDMatch addrMatch
     * with the result
     * @param addrDescriptor1
     * @param addrDescriptor2
     * @param addrMatch
     */
    private void getMatchJava(Mat addrDescriptor1, Mat addrDescriptor2, MatOfDMatch addrMatch) {





    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.show_picture1, menu);
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


	/*JNI functions
	 * we use this to call functions inside jni folder
	 */
    /**
     * Method to call JNI function: getSIFT;
     * Get SIFT keypoints and descriptors
     * @param addrImage address of input image
     * @param addrKeypoint address of output keypoints
     * @param addrDescriptor address of output descriptors
     */
    public static native void getSIFT(String addrImage, long addrKeypoint, long addrDescriptor);
    /**
     * Method to call JNI function: getMATCH;
     * Get matches between SIFT points in both images
     * @param addrDescriptor1 address of descriptors for the first image
     * @param addrDescriptor2 address of descriptors for the second image
     * @param addrMatch address of matching index
     */
    public static native void getMATCH(long addrDescriptor1, long addrDescriptor2, long addrMatch);
    /**
     * Method to call JNI function: getKeypointAndDescriptor;
     * Load SIFT keypoints and descriptors of dataset image
     * @param addrData address of stored data of dataset image
     * @param addrKeypoint address of output keypoints
     * @param addrDescriptor of output descriptors
     */
    public static native void getKeypointAndDescriptor(String addrData, long addrKeypoint, long addrDescriptor);
}
