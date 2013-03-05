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
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import features.DistanceFeature;
import features.EventLemmaFeature;
import features.EventTypeFeature;
import features.InterleavingCommaFeature;
import features.POSFeature;
import features.TempEvalFeature;
import features.TimexTypeFeature;
import features.WindowFeature;

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
	
	private static final String BINARY_CLASSIFIER_FILENAME = 
			"classifiers/same-sentence-event-model-binary.ser.gz";
	private static final String MULTI_CLASSIFIER_FILENAME =
			"classifiers/same-sentence-event-model-multi.ser.gz";
	private static final boolean BINARY = true;
	private static final boolean NOT_BINARY = false;
	
	private double truePositives = 0, falsePositives = 0, trueNegatives = 0, 
			falseNegatives = 0, total = 0;
	private double silverMatches = 0, silverMismatches = 0;
	private double goldMatches = 0, goldMismatches = 0;
	
	private LinearClassifier<String, String> binaryTrainClassifier, multiTrainClassifier,
			binaryTestClassifier, multiTestClassifier;
	private LinearClassifierFactory<String, String> factory;
	private List<Datum<String, String>> binaryTrainingData, multiTrainingData;
	private List<TempEvalFeature> binaryFeatureList, multiFeatureList;

	public SameSentenceEventTagger() {
		factory = new LinearClassifierFactory<String, String>();
		factory.useConjugateGradientAscent();
		factory.setVerbose(true);
		factory.setSigma(10.0);

		binaryTrainingData = new ArrayList<Datum<String, String>>();
		multiTrainingData = new ArrayList<Datum<String, String>>();
		
		binaryFeatureList = new ArrayList<TempEvalFeature>();
		multiFeatureList = new ArrayList<TempEvalFeature>();
		
		//binaryFeatureList.add(new DistanceFeature());
		binaryFeatureList.add(new InterleavingCommaFeature());
		//binaryFeatureList.add(new WindowFeature(3, TextAnnotation.class));
		//binaryFeatureList.add(new EventTypeFeature());
		//binaryFeatureList.add(new POSFeature());
		binaryFeatureList.add(new EventLemmaFeature());
		//binaryFeatureList.add(new WindowFeature(2, PartOfSpeechAnnotation.class));
		
		multiFeatureList.add(new DistanceFeature());
		multiFeatureList.add(new InterleavingCommaFeature());
		multiFeatureList.add(new WindowFeature(3, TextAnnotation.class));
		multiFeatureList.add(new EventTypeFeature());
		multiFeatureList.add(new POSFeature());
		multiFeatureList.add(new EventLemmaFeature());
		multiFeatureList.add(new WindowFeature(2, PartOfSpeechAnnotation.class));
	}
	
	/*
	 * Get all same-sentence event pairs for a document. First element of each pair
	 * occurs before second element in the sentence.
	 */
	private static Set<Pair<CoreLabel, CoreLabel>> getEventPairs(Annotation annotation) {
		Set<Pair<CoreLabel, CoreLabel>> result = new HashSet<Pair<CoreLabel, CoreLabel>>();
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

		// For each sentence, find all timexes and events, and then make pairs
		for(CoreMap sentence: sentences) {

			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);

			// Keep track of all events in this sentence
			List<CoreLabel> events = new ArrayList<CoreLabel>();

			// Keep track of last eid (for multi-token events)
			String lastEid = null;

			// Iterate through tokens and store all events and timexes. Relies on the
			// invariant that no token will be both an event and a timex.
			for (CoreLabel token: tokens) {
				EventInfo eventInfo = token.get(EventAnnotation.class);
				
				// Handle event tokens
				if (eventInfo != null) {

					// Check for multiple tokens in same event tag
					if (lastEid != null && lastEid.equals(eventInfo.currEventId))
						continue;
					lastEid = eventInfo.currEventId;

					events.add(token);
				} else {
					lastEid = null;
				}
			}

			// Make pairs for this sentence
			for (int i = 0; i < events.size(); i++) {
				for (int j = i + 1; j < events.size(); j++) {
					result.add(new Pair<CoreLabel, CoreLabel>(events.get(i), events.get(j)));
				}
			}
		}

		return result;
	}

	/*
	 * Get true same-sentence relationships between events from
	 * the training data. Key string is eiid of first event. Second key string is
	 * eiid of second event. Final value is relationship type.
	 */
	private static Map<String, Map<String, String>> getEventRelationships(Document doc) {
		Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();

		Element root = doc.getDocumentElement();
		Element[] tlinkElems = 
				XMLParser.getElementsByTagNameNR(root, "TLINK");
		for(Element e : tlinkElems) {
			String eventInstanceID = e.getAttribute("eventInstanceID");
			String relatedToEventInstance = e.getAttribute("relatedToEventInstance");
			String relType = e.getAttribute("relType");

			if (eventInstanceID.length() > 0 && relatedToEventInstance.length() > 0
					&& relType.length() > 0) {
				MapUtils.doublePut(result, eventInstanceID, relatedToEventInstance, relType);
			}
		}

		return result;
	}
	
	/*
	 * Creates a single datum (REL or O) from a training example
	 */
	private Datum<String, String> getDatum(CoreLabel eventToken1, CoreLabel eventToken2,
			Set<Pair<CoreLabel, CoreLabel>> pairs, Map<String, Map<String, String>> relationships,
			boolean binary) {

		List<String> features = new ArrayList<String>();

		EventInfo eventInfo1 = eventToken1.get(EventAnnotation.class);
		EventInfo eventInfo2 = eventToken2.get(EventAnnotation.class);

		// FEATURES
		for (TempEvalFeature feature: binaryFeatureList) {
			feature.add(features, eventToken1, eventToken2);
		}

		// LABEL
		String label = MapUtils.doubleGet(relationships, eventInfo1.currEiid, 
				eventInfo2.currEiid);
		//if (!binary && label == null) {
		//	System.out.println("Called getDatum for multi on something with no class.");
		//	System.exit(-1);
		//}
		
		label = (label == null ? "O" : (binary ? "REL" : label)); //label);
		//System.out.println(label);

		return new BasicDatum<String, String>(features, label);
	}
	
	/*
	 * Train classifier on relationships between same-sentence timexes and events.
	 */
	public void train(Annotation annotation, Document doc) {
		// Find all possible same-sentence event pairs
		// Extract JUST the links between events from parsed XML (doc)
		Set<Pair<CoreLabel, CoreLabel>> pairs = getEventPairs(annotation);
		Map<String, Map<String, String>> relationships = getEventRelationships(doc);

		for (Pair<CoreLabel, CoreLabel> pair: pairs) {
			Datum<String, String> binaryDatum = 
					getDatum(pair.first, pair.second, pairs, relationships, BINARY);
			binaryTrainingData.add(binaryDatum);
			if (!binaryDatum.label().equals("O")) {
				Datum<String, String> multiDatum = 
						getDatum(pair.first, pair.second, pairs, relationships, NOT_BINARY);
				multiTrainingData.add(multiDatum);
			}
		}
	}
	
	/*
	 * Zips classifier into file
	 */
	public void doneClassifying() {
		binaryTrainClassifier = factory.trainClassifier(binaryTrainingData);
		multiTrainClassifier = factory.trainClassifier(multiTrainingData);
		LinearClassifier.writeClassifier(binaryTrainClassifier, BINARY_CLASSIFIER_FILENAME);
		LinearClassifier.writeClassifier(multiTrainClassifier, MULTI_CLASSIFIER_FILENAME);
	}

	/*
	 * Loads test classifier from file (presumably written from training)
	 */
	public void loadTestClassifier() {
		binaryTestClassifier = LinearClassifier.readClassifier(BINARY_CLASSIFIER_FILENAME);
		multiTestClassifier = LinearClassifier.readClassifier(MULTI_CLASSIFIER_FILENAME);
	}
	
	/*
	 * Tests classifier
	 */
	public void test(Annotation annotation, Document doc) {
		DocInfo docInfo = annotation.get(DocInfoAnnotation.class);

		// Find all possible same-sentence timex event pairs
		// Extract JUST the links between events and timex's from parsed XML (doc)
		Set<Pair<CoreLabel, CoreLabel>> pairs = getEventPairs(annotation);
		Map<String, Map<String, String>> relationships = getEventRelationships(doc);

		for (Pair<CoreLabel, CoreLabel> pair: pairs) {
			total++;
			Datum<String, String> binaryDatum = 
					getDatum(pair.first, pair.second, pairs, relationships, BINARY);
			String binaryGuess = binaryTestClassifier.classOf(binaryDatum);
			String binaryCorrect = binaryDatum.label();
			
			if (!binaryGuess.equals("O") && !binaryGuess.equals("REL")) {
				System.out.println("bad guess: " + binaryGuess);
				System.exit(-1);
			}
			
			if (!binaryCorrect.equals("O") && !binaryCorrect.equals("REL")) {
				System.out.println("bad correct: " + binaryCorrect);
				System.exit(-1);
			}

			System.out.println(pair.first.get(EventAnnotation.class).currEiid + " "
					+ pair.second.get(EventAnnotation.class).currEiid + " "
					+ binaryGuess + " " + binaryCorrect);
			binaryTestClassifier.justificationOf(binaryDatum);

			// If we think there is some relationship
			if (!binaryGuess.equals("O")) {
				System.out.println("guessed something in file " + docInfo.filename);
				
				Datum<String, String> multiDatum =
						getDatum(pair.first, pair.second, pairs, relationships, NOT_BINARY);
				String multiGuess = multiTestClassifier.classOf(multiDatum);
				String multiCorrect = multiDatum.label();
				
				if (multiGuess.equals(multiCorrect))
					silverMatches++;
				else
					silverMismatches++;
				
				EventInfo info1 = pair.first.get(EventAnnotation.class);
				EventInfo info2 = pair.second.get(EventAnnotation.class);
				LinkInfo link = new LinkInfo("-1", multiGuess, null,
						info1, info2);
				pair.first.set(LinkInfoAnnotation.class, link);
			}
			
			// If there actually is some relationship
			if (!binaryCorrect.equals("O")) {
				Datum<String, String> multiDatum =
						getDatum(pair.first, pair.second, pairs, relationships, NOT_BINARY);
				String multiGuess = multiTestClassifier.classOf(multiDatum);
				String multiCorrect = multiDatum.label();
				
				if (multiGuess.equals(multiCorrect))
					goldMatches++;
				else
					goldMismatches++;
			}
			
			// Update binary stats
			if (binaryGuess.equals("REL") && binaryCorrect.equals("REL"))
				truePositives++;
			else if (binaryGuess.equals("O") && binaryCorrect.equals("O"))
				trueNegatives++;
			else if (binaryGuess.equals("REL") && binaryCorrect.equals("O"))
				falsePositives++;
			else if (binaryGuess.equals("O") && binaryCorrect.equals("REL"))
				falseNegatives++;
			else {
				System.out.println("bad matching: guess " + binaryGuess 
						+ " correct " + binaryCorrect);
				System.exit(-1);
			}
		}
	}
	
	public void printStats() {
		System.out.println("Binary Summary\n-----");
		System.out.println("     TP: " + truePositives);
		System.out.println("     FP: " + falsePositives);
		System.out.println("     TN: " + trueNegatives);
		System.out.println("     FN: " + falseNegatives);
		System.out.println("  Total: " + total);
		System.out.println("     Pr: " + (truePositives / 
				(truePositives + falsePositives)));
		System.out.println("     Re: " + (truePositives / 
				(truePositives + falseNegatives)));
		System.out.println("     F1: " + ((2 * truePositives) / 
				(truePositives + falsePositives + falseNegatives)));
		System.out.println();
		System.out.println("     Gold matches: " + goldMatches);
		System.out.println("  Gold mismatches: " + goldMismatches);
		System.out.println("   Silver matches: " + silverMatches);
		System.out.println("Silver mismatches: " + silverMismatches);
	}
}
