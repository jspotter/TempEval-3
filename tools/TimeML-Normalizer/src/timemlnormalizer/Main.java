package timemlnormalizer;

import java.io.*;
import java.util.*;
import nlp_files.XMLFile;
import org.apache.commons.cli.*;
import utils_bk.*;

/**
 *
 * @author Hector Llorens
 * @since 2011
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String annotations = null;
        ArrayList<File[]> annotationList = new ArrayList<File[]>();

        try {
            long startTime = System.currentTimeMillis();


            Options opt = new Options();
            //addOption(String opt, boolean hasArg, String description)
            opt.addOption("h", "help", false, "Print this help");
            opt.addOption("a", "annotations", true, "List of folders containing annotations of the same docs (between \"\" and separated by ;)");
            opt.addOption("d", "debug", false, "Debug mode: Output errors stack trace (default: disabled)");

            PosixParser parser = new PosixParser();
            CommandLine cl_options = parser.parse(opt, args);
            HelpFormatter hf = new HelpFormatter();
            if (cl_options.hasOption('h')) {
                hf.printHelp("TimeML-Normalizer", opt);
                System.exit(0);
            } else {
                if (cl_options.hasOption('d')) {
                    System.setProperty("DEBUG", "true");
                }
                String[] annotationsarr = null;

                if (cl_options.hasOption('a')) {
                    annotations = cl_options.getOptionValue("a");
                    annotationsarr = annotations.split(";");
                    if (annotationsarr.length < 2) {
                        hf.printHelp("TimeML-Normalizer", opt);
                        throw new Exception("At least TWO annnotations are required.");
                    }
                    for (int i = 0; i < annotationsarr.length; i++) {
                        File f = new File(annotationsarr[i]);
                        if (!f.exists()) {
                            hf.printHelp("TimeML-Normalizer", opt);
                            throw new Exception("Annotation does not exist: " + annotationsarr[i]);
                        }
                        if (f.isFile()) {
                            File[] files = {f};
                            XMLFile xmlfile = new XMLFile();
                            xmlfile.loadFile(f);
                            xmlfile.overrideExtension("tml-min-consistency");
                            if (!xmlfile.isWellFormed()) {
                                throw new Exception("File: " + xmlfile.getFile().getCanonicalPath() + " is not a valid TimeML XML file.");
                            }
                            annotationList.add(files);
                        } else {
                            File[] files = f.listFiles(FileUtils.onlyFilesFilter);
                            if (files.length == 0) {
                                throw new Exception("Empty folder: " + f.getName());
                            }
                            for (int fn = 0; fn < files.length; fn++) {
                                XMLFile xmlfile = new XMLFile();
                                xmlfile.loadFile(files[fn]);
                                xmlfile.overrideExtension("tml-min-consistency");
                                if (!xmlfile.isWellFormed()) {
                                    throw new Exception("File: " + xmlfile.getFile().getCanonicalPath() + " is not a valid TimeML XML file.");
                                }
                            }
                            annotationList.add(files);
                        }
                        if (i > 0) {
                            // check equal to previous
                            File[] files1 = annotationList.get(i - 1);
                            File[] files2 = annotationList.get(i);

                            if (files1.length != files2.length) {
                                throw new Exception("Annotation folders must contain exactly the same number of files: " + files1.length);
                            } else {
                                for (int fn = 0; fn < files1.length; fn++) {
                                    if (!files1[fn].getName().equals(files2[fn].getName())) {
                                        throw new Exception("Annotation folders must contain exactly the same files: " + files1[fn]);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    hf.printHelp("TimeML-Normalizer", opt);
                    throw new Exception("Annotations parameter is required.");
                }
            }

            TimeML_Normalizer.normalize(annotationList);

            long endTime = System.currentTimeMillis();
            long sec = (endTime - startTime) / 1000;
            if (sec < 60) {
                System.err.println("Done in " + StringUtils.twoDecPosS(sec) + " sec!\n");
            } else {
                System.err.println("Done in " + StringUtils.twoDecPosS(sec / 60) + " min!\n");
            }
        } catch (Exception e) {
            System.err.println("Errors found:\n\t" + e.getMessage() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
            }
            System.exit(1);
        }

    }
}
