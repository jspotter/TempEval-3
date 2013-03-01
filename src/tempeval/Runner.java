package tempeval;

import helperclasses.AnnotationWriter;
import helperclasses.XMLParser;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;

import annotationclasses.DocInfoAnnotation;

import dataclasses.DocInfo;


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

	private static final String TRAIN_DIR = 
			"data/TBAQ-cleaned/AQUAINT";
	private static final String OUTPUT_DIR =
			"output/TBAQ-cleaned/AQUAINT";
	
	private static final String EVENT_TRAIN_FILE = 
			"classifiers/training/event.out";

	private static StanfordCoreNLP pipeline;

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
		
		br.close();

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
	 * Builds up annotation object wih built in CoreNLP annotations as
	 * well as events.
	 */
	private static void train() throws Exception {
		
		BufferedWriter event_train_out = new BufferedWriter(new FileWriter(EVENT_TRAIN_FILE));
		TimexEventTagger.initTagger();

		// Read each training file in training directory
		int numFiles = 0;
		File directory = new File(TRAIN_DIR);
		for (File child : directory.listFiles()) {
			if (child.getName().startsWith("."))
				continue;
			
			System.out.println("Training on file " + child.getName());
			
			// Parse XML
			Document doc = XMLParser.parse(child);
			
			String trainingText = getTrainingText(child);
			trainingText = XMLParser.getRawTextByTagName(trainingText, "<TEXT>", "</TEXT>");
			Annotation annotation = new Annotation(getTrainingText(child));
			pipeline.annotate(annotation);

			// Annotate with events
			EventTagger.annotate(annotation, doc);

			//Print out file to train classifier upon
			EventTagger.printEventAnnotations(annotation, event_train_out);
			
			// Annotate with same-sentence event-timex pairs
			TimexEventTagger.trainEventTimex(annotation, doc);
			
			if (++numFiles >= 10) break;
		}
		event_train_out.close();
		TimexEventTagger.doneClassifying();
	}
	
	private static void addDocumentInfo(Annotation annotation, Document doc, 
			String rawText, String filename) {
		Element root = doc.getDocumentElement();
		String id = XMLParser.getElementTextByTagNameNR(root, "DOCID");
		String dct = XMLParser.getRawTextByTagName(rawText, "<DCT>", "</DCT>");
		String title = XMLParser.getElementTextByTagNameNR(root, "TITLE");
		String extra = XMLParser.getElementTextByTagNameNR(root, "EXTRAINFO");
		DocInfo info = new DocInfo(filename, id, dct, title, extra);
		annotation.set(DocInfoAnnotation.class, info);
	}
	
	private static void test() throws Exception {
		EventTagger.loadTestClassifier();
		TimexEventTagger.loadTestClassifier();

		// Test
		int numFiles = 0;
		File directory = new File(TRAIN_DIR);
		for (File child : directory.listFiles()) {
			if (child.getName().startsWith("."))
				continue;
			
			System.out.println("Testing on file " + child.getName());
			
			// Parse XML
			Document doc = XMLParser.parse(child);
			String rawText = getTrainingText(child);

			// Do initial annotation
			Annotation annotation = new Annotation(getTestingText(doc));
			pipeline.annotate(annotation);
			
			// Add document information
			addDocumentInfo(annotation, doc, rawText, child.getName());

			// Annotate with events
			EventTagger.testEventTagger(annotation);

			// Annotate with same-sentence event-timex pairs
			TimexEventTagger.testEventTimex(annotation, doc);
			
			// Write this annotation
			BufferedWriter out = new BufferedWriter(new FileWriter(OUTPUT_DIR
					+ "/" + child.getName()));
			AnnotationWriter.writeAnnotation(annotation, out);
			out.close();
			
			if (++numFiles >= 10) break;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		// Create pipeline
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		//props.put("annotators", "tokenize, ssplit");
		pipeline = new StanfordCoreNLP(props);

		train();
		test();
	}
}
