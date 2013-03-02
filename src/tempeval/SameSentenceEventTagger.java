package tempeval;

import dataclasses.AuxTokenInfo;
import dataclasses.DocInfo;
import dataclasses.EventInfo;
import dataclasses.LinkInfo;
import dataclasses.TimeInfo;
import edu.stanford.nlp.classify.*;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

import org.w3c.dom.*;

import annotationclasses.AuxTokenInfoAnnotation;
import annotationclasses.DocInfoAnnotation;
import annotationclasses.EventAnnotation;
import annotationclasses.LinkInfoAnnotation;
import annotationclasses.TimeAnnotation;

import helperclasses.MapUtils;
import helperclasses.XMLParser;

import java.io.*;
import java.util.*;

public class SameSentenceEventTagger {
	
	private static final String CLASSIFIER_FILENAME = 
			"classifiers/same-sentence-event-model.ser.gz";
	
	private static LinearClassifier<String, String> trainClassifier, testClassifier;
	private static LinearClassifierFactory<String, String> factory;
	private static List<Datum<String, String>> trainingData;

	public static void initTagger() {
		factory = new LinearClassifierFactory<String, String>();
		factory.useConjugateGradientAscent();
		factory.setVerbose(true);
		factory.setSigma(10.0);

		trainingData = new ArrayList<Datum<String, String>>();
	}
	
	/*
	 * TODO
	 */
	public static void train(Annotation annotation, Document doc) {
		
		
	}
	
	/*
	 * TODO
	 */
	public static void test(Annotation annotation, Document doc) {


	}
	
	/*
	 * TODO
	 */
	public static void loadTestClassifier() {
		
	}
	
	/*
	 * TODO
	 */
	public static void doneClassifying() {

	}
}
