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
	private static final String outputdir =
			"output/TBAQ-cleaned/AQUAINT";

	private static StanfordCoreNLP pipeline;
	private static ArrayList<Annotation> annotations;

	/*
	 * Gets training text. This is everything in the <TEXT> tag INCLUDING
	 * EVENT, TIMEX, and SIGNAL tags themselves.
	 */
	private static String getTrainingText(File child) throws IOException {
		String fileText = "";
		String currLine;
		BufferedReader br = new BufferedReader(new FileReader(child));

		while ((currLine = br.readLine()) != null) {
			fileText += currLine;
		}

		//Concealed Hacky way of getting training file content
		fileText = XMLParser.getRawTextByTagName(fileText, "<TEXT>", "</TEXT>");
		return fileText;
	}

	/*
	 * Gets test text. This is everything in the <TEXT> tag EXCLUDING
	 * EVENT, TIMEX, and SIGNAL tags themselves.
	 */
	private static String getTestingText(Document doc) {
		return XMLParser.getElementTextByTagNameNR(doc.getDocumentElement(), "TEXT");
	}

	/*
	 * Builds up annotation object with built in CoreNLP annotations as
	 * well as events.
	 */
	private static void train() throws Exception {

		TimexEventTagger.initTagger();

		// Read each training file in training directory
		int filesRead = 0;
		File directory = new File(traindir);
		for (File child : directory.listFiles()) {
			if (child.getName().startsWith("."))
				continue;
			
			// Parse XML
			Document doc = XMLParser.parse(child);
			
			Annotation annotation = new Annotation(getTrainingText(child));
			pipeline.annotate(annotation);

			// Annotate with events
			EventTagger.annotate(annotation, doc);

			// Annotate with same-sentence event-timex pairs
			TimexEventTagger.trainEventTimex(annotation, doc);

			// Finally, add this annotation as a training example
			annotations.add(annotation);
		}

		TimexEventTagger.doneClassifying();
	}
	
	private static void addDocumentInfo(Annotation annotation, Document doc) {
		DocInfo info = new DocInfo("", "", "", ""); //TODO add actual document info
		annotation.set(DocInfoAnnotation.class, info);
	}
	
	private static void test() throws Exception {
		EventTagger.loadTestClassifier();
		TimexEventTagger.loadTestClassifier();

		// Test
		File directory = new File(traindir);
		for (File child : directory.listFiles()) {
			if (child.getName().startsWith("."))
				continue;
			
			// Parse XML
			Document doc = XMLParser.parse(child);

			// Do initial annotation
			Annotation annotation = new Annotation(getTestingText(doc));
			pipeline.annotate(annotation);
			
			// Add document information
			addDocumentInfo(annotation, doc);

			// Annotate with events
			EventTagger.testEventTagger(annotations);

			// Annotate with same-sentence event-timex pairs
			TimexEventTagger.testEventTimex(annotation, doc);
		}

		TimexEventTagger.doneTesting(); //TODO remove this eventually

		// Write annotations
		/*try {
			BufferedWriter out = new BufferedWriter(new FileWriter("sample.out"));
			for(Annotation a: annotations) {
				EventTagger.printEventAnnotations(a, out);
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		// Create pipeline
		Properties props = new Properties();
		//props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		props.put("annotators", "tokenize, ssplit");
		pipeline = new StanfordCoreNLP(props);
		annotations = new ArrayList<Annotation>();

		train();
		test();
	}
}
