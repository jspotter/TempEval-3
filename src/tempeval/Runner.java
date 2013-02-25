package tempeval;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

public class Runner {

	private static final String traindir = 
		"data/TBAQ-cleaned/AQUAINT";
	private static final String trainfile =
		"eventdata/event-train.txt";
	private static final String testfile =
		"eventdata/event-test.txt";
	
	private static StanfordCoreNLP pipeline;
	
	private static void train() {
		BufferedWriter trainwriter = null, testwriter = null;
		try {
			trainwriter = new BufferedWriter(
					new FileWriter(trainfile));
			testwriter = new BufferedWriter(
					new FileWriter(testfile));
		} catch(IOException e) {
			System.out.println("Unable to open output files.");
			e.printStackTrace();
			System.exit(-1);
		}
		
		File directory = new File(traindir);
		for (File child : directory.listFiles()) {
			if (child.getName().startsWith("."))
				continue;
			
			System.out.println("Training on file " + child.getName());
			String text = "";
			try {
				BufferedReader rd = new BufferedReader(new FileReader(child));
				while (true) {
					String line = rd.readLine();
					if (line == null) break;
					text += line;
				}
			} catch(IOException e) {
				System.out.println("*** Unable to read file. ***");
				e.printStackTrace();
				continue;
			}
			
			//System.out.println(text.indexOf("<TEXT>") + " " + text.indexOf("</TEXT>"));
			text = text.substring(text.indexOf("<TEXT>") + 6,
					text.indexOf("</TEXT>"));
	
			// Annotate
			Annotation annotation = new Annotation(text);
			pipeline.annotate(annotation);
	
			// Train event tagger
			EventTagger.train(pipeline, annotation, trainwriter, testwriter);
		}
		
		try {
			trainwriter.close();
			testwriter.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		
		// Create pipeline
		Properties props = new Properties();
		//props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		props.put("annotators", "tokenize, ssplit");
		pipeline = new StanfordCoreNLP(props);

		// Get XML file
		//Document trainDoc = XMLParser.parse(trainfile);
		//Document testDoc = XMLParser.parse(testfile);

		// Get text of XML file
		//Element root = trainDoc.getDocumentElement();
		//Element[] texts = XMLParser.getElementsByTagNameNR(root, "TEXT");
		//Element text = texts[0];
		
		train();
	}
}
