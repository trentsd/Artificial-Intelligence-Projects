import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;

public class Thermostat {

    private static final int MAX_ITERATIONS = 1000;
    private static final double ALPHA = 0.007;
    private static final double DERR = 0.001;
    private static final double K = 0.2;

    private static double tempMax;
    private static double tempMin;
    private static double tempRange;
    private double[] temps1;
    private double[] temps2;
    private double[] temps3;
    private double[][] temps0;
    private double[][] tempsNorm;
    private double[] temps4;
    private double[][] denormed;

    /**
     * Constructor. Combines data from 3 train days into a single 2D array temps0. Creates a normalized version of the 2D array tempsNorm.
     *
     * @param train1 explicit path of txt files as strings ("src/train_data_1.txt")
     * @param train2 see above
     * @param train3 see above
     * @param test   see above
     */
    public Thermostat(String train1, String train2, String train3, String test) {
        try {
            this.temps1 = readIn(train1);
            this.temps2 = readIn(train2);
            this.temps3 = readIn(train3);
            this.temps4 = readIn(test);
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
            System.exit(-1);
        }

        this.tempMax = Double.MIN_VALUE;
        this.tempMin = Double.MAX_VALUE;
        this.tempRange = 0;

        //Make the datasets usable in supervised learning by shifting, then adding it all to one dataset
        double[][] day1 = shift(this.temps1);
        double[][] day2 = shift(this.temps2);
        double[][] day3 = shift(this.temps3);
        this.temps0 = append(day1, day2, day3);

        //Normalize the data, sets the tempMin, tempMax, and tempRange fields
        this.tempsNorm = normalize(this.temps0);

        //todo change me
        //this.denormed = denormalize(this.tempsNorm);


    }

    /**
     * Normalizes the temperature values in between 0 and 1
     *
     * @param temps the 2D array of values from text files
     * @return a version of temps0 where temperature values have been normalized
     */
    public double[][] normalize(double[][] temps) {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        double range = 0;
        int i, j;
        double[][] result = temps.clone();
        for (i = 0; i < temps.length; i++) {
            for (j = 0; j < temps[i].length; j++) {
                if (temps[i][j] > max) {
                    max = temps[i][j];
                }
                if (temps[i][j] < min) {
                    min = temps[i][j];
                }
            }
        }
        range = max - min;

        //Sets fields, this method is only called in constructor so it is fine
        this.tempMax = max;
        this.tempMin = min;
        this.tempRange = range;

        for (i = 0; i < result.length; i++) {
            for (j = 0; j < result[i].length; j++) {
                result[i][j] = (result[i][j] - min) / range;
                System.out.println("I AM HERE " + result[i][j]);
            }
        }

        return result;
    }

    /**
     * Denormalizes the 2d array of data
     *
     * @param norms the normalized data
     * @return
     */
    public static double[][] denormalize(double[][] norms) {
        int i, j;
        double[][] result = norms.clone();
        for (i = 0; i < result.length; i++) {
            result[i][1] = (result[i][1] * 33) + 59;
        }
        return result;
    }

    /**
     * reads in the temperature values from text files to be stored in arrays temps1, temps2, and temps3
     *
     * @param txt the file name as a string
     *            e.g."src/train_data_1.txt"
     * @return array of just temperature values
     * @throws FileNotFoundException
     */
    public double[] readIn(String txt) throws FileNotFoundException {
        File file = new File(txt);
        Scanner scan = new Scanner(file);
        String buffer;
        String[] timeTemp;
        double[] data = new double[9];
        for (int i = 0; i < 9; i++) {
            buffer = scan.nextLine();
            timeTemp = buffer.split(",");
            data[i] = Double.parseDouble(timeTemp[1]);
        }
        return data;
    }

    /**
     * Transforms the data of one day into something that can be used in supervised learning,
     * preceding temperatures are the input and successive temperatures are desired output.
     *
     * @param data the temperatures from a single day
     * @return 2D array of preceding temperatures in column 0 and successive temperatures in column 1
     */
    public double[][] shift(double[] data) {
        double[][] result = new double[8][2];
        result[0][0] = data[0];
        result[0][1] = data[1];
        for (int i = 0; i < data.length - 1; i++) {
            result[i][0] = data[i];
            result[i][1] = data[i + 1];
        }
        return result;
    }

    /**
     * Helper method to append all shifted 2d arrays from the 3 days into one 2d array
     *
     * @param day1
     * @param day2
     * @param day3
     * @return one complete 2d array of all shifted data
     */
    public double[][] append(double[][] day1, double[][] day2, double[][] day3) {
        double[][] result = new double[day1.length + day2.length + day3.length][];
        System.arraycopy(day1, 0, result, 0, day1.length);
        System.arraycopy(day2, 0, result, day1.length, day2.length);
        System.arraycopy(day3, 0, result, day1.length + day2.length, day3.length);
        return result;
    }

    /**
     * Implementation of the delta learning algorithm. Adjusts learning by the value of delta, which is returned by fbips
     *
     * @param temps normalized values for the first 3 days of temperature
     * @return the weights corresponding the the separation line, also printed to sep_line.txt
     * @throws FileNotFoundException thrown because of file writing
     */
    public static double[] deltaLearning(double[][] temps) throws FileNotFoundException {
        int i, j;
        double[][] pats = new double[temps.length][3];
        double[] dOut = new double[temps.length];
        //set up pats (patterns) and dOut (desired out) in the following format [hour][last_temp][bias], [target_temp]
        for (i = 0; i < pats.length; i++) {
            if (i < 8) {
                pats[i][0] = i + 6;
                pats[i][1] = temps[i][0];
            } else if (i < 16) {
                pats[i][0] = i - 2;
                pats[i][1] = temps[i][0];
            } else {
                pats[i][0] = i - 10;
                pats[i][1] = temps[i][0];
            }
            pats[i][2] = 1;
            dOut[i] = temps[i][1];
        }

        for (i = 0; i < pats.length; i++) {
            for (j = 0; j < pats[i].length; j++) {
                System.out.print(pats[i][j] + " ");
            }
            System.out.println();
        }

        //set up out (actual output) and initialize weights to random values between 0 and 1
        double[] out = new double[temps.length];
        Random random = new Random();
        double[] weights = new double[3];
        weights[0] = random.nextDouble();
        weights[1] = random.nextDouble();
        weights[2] = random.nextDouble();
        //set up delo to hold derivation of activation function
        double[] delo = new double[temps.length];
        //array to hold return values of activation function
        double[] act = new double[2];

        //Begin iterating through deltaLearning algorithm, calculating error and adjusting weights
        int iteration;
        int p;
        double net, err, te, learn, gradient;
        net = err = te = learn = gradient = Double.NaN;
        //Initialize output string and printwriter for output table and separation line
        String text = "Hodor";
        PrintWriter writer = new PrintWriter("src/outputs.txt");
        for (iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            //Initialize te (total error)
            te = 0;
            gradient = 0;
            for (p = 0; p < pats.length; p++) {
                //Reset net to 0 before iterating through each pattern to get net
                net = 0;
                for (i = 0; i < 3; i++) {
                    net = net + (weights[i] * pats[p][i]);
                }
                //Result of activation function and its derivation stored in out[p] and delo[p] NO ACTIVATION FUNCTION, JUST NET
                //act = fbip(net);
                out[p] = net;
                //Calculate err (error)
                err = dOut[p] - net;
                //Caclulate te (total error)
                te = te + (err * err);
                //gradient = -1 * gradient+(2*err*
                //Adjust learn (learning signal)
                learn = -ALPHA * err;
                //Adjust weights for number of augmented inputs (pats[p].length) should be 3
                for (i = 0; i < 3; i++) {
                    weights[i] = weights[i] + (2 * ALPHA * err * pats[p][i]);
                }
                if (iteration == 0 || iteration == 999) {
                    text = String.format("iteration= %d, p= %d, output= %5.2f, desired output= %5.2f, delta= %5.2f, error= %6.3f, total error= %6.3f",
                            iteration, p, out[p], dOut[p], delo[p], err, te);
                    writer.println(text);
                }
            }
            if (te < DERR) {
                break;
            }
        }
        writer.close();

        writer = new PrintWriter("src/sep_line.txt");
        writer.printf("%f%n%f%n%f%n", weights[0], weights[1], weights[2]);
        writer.close();

        return weights;
    }

    /**
     * Activation function used in delta learning. This is the only function in the hidden layer of this network. Represents tanh (hyperbolic tangent) and its derivative.
     *
     * @param net
     * @return
     */
    public static double[] fbip(double net) {
        double[] result = new double[2];
        result[0] = (2 / (1 + Math.exp(-2 * K * net))) - 1;
        result[1] = K * (1 - (result[0] * result[0]));
        return result;
    }

    /**
     * Linear activation function f(a) = a. Written in this format as to be easily switched with fbip(net) in the deltaLearning() function
     *
     * @param net
     * @return just gives back net and a slope of 1
     */
    public static double[] linearActivation(double xVal, double[] weights) {
        double m, x, b;
        x = xVal;
        double[] result = new double[2];
        m = -(weights[0]) / weights[1];
        b = -(weights[2]) / weights[1];
        result[0] = (m * x) + b;
        result[1] = m;
        return result;
    }

    public static double[][] predict(double[] weights) {
        double m, x, b;
        double[][] result = new double[9][2];
        m = (weights[0] / weights[1]);
        b = (weights[2] / weights[1]);
        for (int i = 0; i < 9; i++) {
            x = i + 5;
            result[i][0] = x;
            result[i][1] = (m * x) + b;
        }
        return result;
    }

    public static void analyze(double[][] predictions, double[] actual) throws FileNotFoundException{
        PrintWriter writer = new PrintWriter("src/testingResults.txt");
        String text = "Hodor";
        for (int i=0;i<predictions.length;i++){
            double predicted = predictions[i][1];
            double actualVal = actual[i];
            double error = predicted - actualVal;
            double percentError = Math.abs(error/actualVal)*100;
            text = String.format("predicted output= %5.2f, actual output= %5.2f, error= %5.2f, percent error= %5.2f",
                    predicted, actualVal, error, percentError);
            writer.println(text);
        }
        writer.close();
    }

    public static void main(String[] args) {
        Thermostat test = new Thermostat("src/train_data_1.txt", "src/train_data_2.txt", "src/train_data_3.txt", "src/test_data_4.txt");

        try {
            //double[] weights = deltaLearning(test.tempsNorm);
            double[] weights = {0.071014, 0.603732, -0.370911};
            double[][] prediction = predict(weights);
            for (int i = 0; i < prediction.length; i++) {
                for (int j = 0; j < prediction[i].length; j++) {
                    System.out.print(prediction[i][j] + " ");
                }
                System.out.println();
            }
            double[][] denormed = denormalize(prediction);
            for (int i = 0; i < denormed.length; i++) {
                for (int j = 0; j < denormed[i].length; j++) {
                    System.out.print(denormed[i][j] + " ");
                }
                System.out.println();
            }
            analyze(denormed, test.temps4);
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
            System.exit(-1);
        }
    }


}
