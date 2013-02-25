package timemlnormalizer;

import java.io.*;
import utils_bk.FileUtils;

/**
 * Tokenizer.java
 * @author Hector Llorens
 * @since Oct 27, 2011
 */
public class Tokenizer {

    private static String program_path = FileUtils.getApplicationPath() + "program-data"+File.separator+ "tokenizer"+File.separator;
    private static String program_bin = program_path + "tokenize.pl";
    private static String abbrv = program_path + "english-abbreviations";


    /**
     * Runs the tokenizer over a file and saves file in .tok
     *
     * @param filename
     * @return Output filename
     */
    public static String run(String filename) {
        execute(filename, filename + ".tok");
        return filename + ".tok";
    }

    private static void execute(String filename, String outputfile) {
        try {
            String[] command = {"/bin/sh", "-c",program_bin+" -e -a \""+abbrv+"\" \"" + filename +"\" | sed \"s/[[:blank:]]\\+//g\" | grep -v '^$'"};
            Process p = Runtime.getRuntime().exec(command);

            String line;
            BufferedWriter output = new BufferedWriter(new FileWriter(outputfile));
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                while ((line = stdInput.readLine()) != null) {
                            line=line.trim().replaceAll("\\|", "-");
                            if (line.length()>0) {
                                        output.write(line + "\n");
                            }
                }
            } finally {
                if (stdInput != null) {
                    stdInput.close();
                }
                if (output != null) {
                    output.close();
                }
                if(p!=null){
                    p.getInputStream().close();
                    p.getOutputStream().close();
                    p.getErrorStream().close();
                    p.destroy();
                }
            }

        } catch (Exception e) {
            System.err.println("Errors found (TreeTagger):\n\t" + e.toString());
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }

        }


    }












}
