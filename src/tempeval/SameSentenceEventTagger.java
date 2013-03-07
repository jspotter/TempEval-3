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
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import features.DistanceFeature;
import features.EventLemmaFeature;
import features.EventTypeFeature;
import features.HeadingPrepFeature;
import features.InterleavingCommaFeature;
import features.IntervalFeature;
import features.POSFeature;
import features.SyntacticDominanceFeature;
import features.SyntacticRelationFeature;
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
import helperclasses.TestUtils;
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
	private LinearClassifierFactory<String, String> trainFactory, testFactory;
	private List<Datum<String, String>> binaryTrainingData, multiTrainingData;
	private List<TempEvalFeature> binaryFeatureList, multiFeatureList;
	
	private PrintWriter outWriter;

	public SameSentenceEventTagger() throws IOException {
		File outFile = new File("extras/same-sentence-output-" + System.currentTimeMillis() + ".txt");
		outWriter = new PrintWriter(new FileWriter(outFile));
		
		trainFactory = new LinearClassifierFactory<String, String>();
		trainFactory.useConjugateGradientAscent();
		trainFactory.setVerbose(true);
		trainFactory.setSigma(10.0);
		
		testFactory = new LinearClassifierFactory<String, String>();
		testFactory.useConjugateGradientAscent();
		testFactory.setVerbose(true);
		testFactory.setSigma(10.0);

		binaryTrainingData = new ArrayList<Datum<String, String>>();
		multiTrainingData = new ArrayList<Datum<String, String>>();
		
		binaryFeatureList = new ArrayList<TempEvalFeature>();
		multiFeatureList = new ArrayList<TempEvalFeature>();
		
		// Here is where you add features for relationship/no relationship
		
		binaryFeatureList.add(new DistanceFeature());
		binaryFeatureList.add(new InterleavingCommaFeature());
		////binaryFeatureList.add(new WindowFeature(3, LemmaAnnotation.class));
		////binaryFeatureList.add(new EventTypeFeature());
		binaryFeatureList.add(new POSFeature());
		////binaryFeatureList.add(new EventLemmaFeature());
		binaryFeatureList.add(new WindowFeature(2, PartOfSpeechAnnotation.class));
		binaryFeatureList.add(new HeadingPrepFeature());
		////binaryFeatureList.add(new IntervalFeature());
		binaryFeatureList.add(new SyntacticRelationFeature());
		binaryFeatureList.add(new SyntacticDominanceFeature());
		
		// Here is where you add features for kind of relationship
		// (given that there is one)
		
		////multiFeatureList.add(new DistanceFeature());
		////multiFeatureList.add(new InterleavingCommaFeature());
		multiFeatureList.add(new WindowFeature(3, LemmaAnnotation.class));
		multiFeatureList.add(new EventTypeFeature());
		multiFeatureList.add(new POSFeature());
		multiFeatureList.add(new EventLemmaFeature());
		multiFeatureList.add(new WindowFeature(2, PartOfSpeechAnnotation.class));
		multiFeatureList.add(new HeadingPrepFeature());
		////multiFeatureList.add(new IntervalFeature());
		multiFeatureList.add(new SyntacticRelationFeature());
		multiFeatureList.add(new SyntacticDominanceFeature());
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
	private static Map<String, Map<String, String>> getEventRelationships(Document doc,
			Map<String, String> eiidMappings) {
		Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();

		// Get TLINKs
		Element root = doc.getDocumentElement();
		Element[] tlinkElems = 
				XMLParser.getElementsByTagNameNR(root, "TLINK");
		for (Element e : tlinkElems) {
			String eventInstanceID = e.getAttribute("eventInstanceID");
			String relatedToEventInstance = e.getAttribute("relatedToEventInstance");
			String relType = e.getAttribute("relType");

			if (eventInstanceID.length() > 0 && relatedToEventInstance.length() > 0
					&& relType.length() > 0) {
				
				if (eiidMappings != null) {
					eventInstanceID = eiidMappings.get(eventInstanceID);
					relatedToEventInstance = eiidMappings.get(relatedToEventInstance);
				}
				
				MapUtils.doublePut(result, eventInstanceID, relatedToEventInstance, relType);
			}
		}
		
		// Get SLINKs
		Element[] slinkElems =
				XMLParser.getElementsByTagNameNR(root, "SLINK");
		for (Element e : slinkElems) {
			String eventInstanceID = e.getAttribute("eventInstanceID");
			String subordinatedEventInstance = e.getAttribute("subordinatedEventInstance");
			String relType = e.getAttribute("relType");
			
			if (eventInstanceID.length() > 0 && subordinatedEventInstance.length() > 0
					&& relType.length() > 0) {
				
				if (eiidMappings != null) {
					eventInstanceID = eiidMappings.get(eventInstanceID);
					subordinatedEventInstance = eiidMappings.get(subordinatedEventInstance);
				}
				
				MapUtils.doublePut(result, eventInstanceID, subordinatedEventInstance, relType);
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
		AuxTokenInfo aux1 = eventToken1.get(AuxTokenInfoAnnotation.class);
		AuxTokenInfo aux2 = eventToken2.get(AuxTokenInfoAnnotation.class);
		
		assert (aux1.tokenOffset < aux2.tokenOffset);

		// FEATURES
		List<TempEvalFeature> featureList = (binary ? binaryFeatureList : multiFeatureList);
		for (TempEvalFeature feature: featureList) {
			feature.add(features, eventToken1, eventToken2);
		}

		// LABEL
		String label = MapUtils.doubleGet(relationships, eventInfo1.currEiid, 
				eventInfo2.currEiid);
		if (label == null)
			label = MapUtils.doubleGet(relationships, eventInfo2.currEiid,
					eventInfo1.currEiid);
		
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
		Map<String, Map<String, String>> relationships = getEventRelationships(doc, null);

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
		binaryTrainClassifier = trainFactory.trainClassifier(binaryTrainingData);
		multiTrainClassifier = testFactory.trainClassifier(multiTrainingData);
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
	public void test(Annotation annotation, Annotation goldenAnnotation, Document doc) 
			throws IOException {
		DocInfo docInfo = annotation.get(DocInfoAnnotation.class);

		// Find all possible same-sentence timex event pairs
		// Extract JUST the links between events and timex's from parsed XML (doc)
		Set<Pair<CoreLabel, CoreLabel>> pairs = getEventPairs(annotation);
		Map<String, String> eiidMappings = TestUtils.getEiidMappings(annotation, goldenAnnotation);
		Map<String, Map<String, String>> relationships = getEventRelationships(doc, eiidMappings);

		// For each pari
		for (Pair<CoreLabel, CoreLabel> pair: pairs) {
			total++;
			
			// Get guess and correct for "REL" or "O"
			Datum<String, String> binaryDatum = 
					getDatum(pair.first, pair.second, pairs, relationships, BINARY);
			String binaryGuess = binaryTestClassifier.classOf(binaryDatum);
			binaryTestClassifier.justificationOf(binaryDatum, outWriter);
			Counter<String> probs = binaryTestClassifier.probabilityOf(binaryDatum);
			double scoreOfNone = probs.getCount("O");
			
			// Break ties by saying "O"
			if (scoreOfNone > 0.4999 && scoreOfNone < 0.5001)
				binaryGuess = "O";
			String binaryCorrect = binaryDatum.label();
			
			// Stupid error checking
			if (!binaryGuess.equals("O") && !binaryGuess.equals("REL")) {
				System.out.println("bad guess: " + binaryGuess);
				System.exit(-1);
			}
			if (!binaryCorrect.equals("O") && !binaryCorrect.equals("REL")) {
				System.out.println("bad correct: " + binaryCorrect);
				System.exit(-1);
			}
			
			// Get all this junk
			EventInfo info1 = pair.first.get(EventAnnotation.class);
			EventInfo info2 = pair.second.get(EventAnnotation.class);
			AuxTokenInfo aux1 = pair.first.get(AuxTokenInfoAnnotation.class);
			AuxTokenInfo aux2 = pair.second.get(AuxTokenInfoAnnotation.class);
			String word1 = pair.first.get(TextAnnotation.class);
			String word2 = pair.second.get(TextAnnotation.class);

			// If we think there is some relationship
			Datum<String, String> multiDatum = null;
			if (!binaryGuess.equals("O")) {				
				multiDatum =
						getDatum(pair.first, pair.second, pairs, relationships, NOT_BINARY);
				String multiGuess = multiTestClassifier.classOf(multiDatum);
				String multiCorrect = multiDatum.label();
				
				if (multiGuess.equals(multiCorrect))
					silverMatches++;
				else
					silverMismatches++;
				
				LinkInfo link = new LinkInfo("-1", multiGuess, null,
						info1, info2);
				pair.first.set(LinkInfoAnnotation.class, link);
			}
			
			// If there actually is some relationship
			if (!binaryCorrect.equals("O")) {
				multiDatum =
						getDatum(pair.first, pair.second, pairs, relationships, NOT_BINARY);
				String multiGuess = multiTestClassifier.classOf(multiDatum);
				multiTestClassifier.justificationOf(multiDatum, outWriter);
				String multiCorrect = multiDatum.label();
				
				if (multiGuess.equals(multiCorrect)) {
					goldMatches++;
					outWriter.write("Gold Match: " + word1 + " " + word2 + " (" +
							multiGuess + ")\n");
					outWriter.write(aux1.tree.toString() + "\n");
				} else {
					goldMismatches++;
					outWriter.write("Gold Mismatch: " + word1 + " " + word2 + " (" +
							multiGuess + ", " + multiCorrect + ")\n");
					outWriter.write(aux1.tree.toString() + "\n");
				}
			}
			
			// Update binary stats
			if (binaryGuess.equals("REL") && binaryCorrect.equals("REL")) {
				truePositives++;
				outWriter.write("Precisely: event " + info1.currEiid 
						+ " (" + word1 + ") and event " + info2.currEiid + " ("
						+ word2 + ") [" + multiDatum.label() + "] - " + docInfo.filename + "\n");
				outWriter.write(aux1.tree.toString() + "\n");
			} else if (binaryGuess.equals("O") && binaryCorrect.equals("O"))
				trueNegatives++;
			else if (binaryGuess.equals("REL") && binaryCorrect.equals("O")) {
				falsePositives++;
				outWriter.write("Precision error: event " + info1.currEiid 
						+ " (" + word1 + ") and event " + info2.currEiid + " ("
						+ word2 + ") - " + docInfo.filename + "\n");
				outWriter.write(aux1.tree.toString() + "\n");
			} else if (binaryGuess.equals("O") && binaryCorrect.equals("REL")) {
				falseNegatives++;
				outWriter.write("Recall error: event " + info1.currEiid 
						+ " (" + word1 + ") and event " + info2.currEiid + " ("
						+ word2 + ") [" + multiDatum.label() + "] - " + docInfo.filename + "\n");
				outWriter.write(aux1.tree.toString() + "\n");
			} else {
				System.out.println("bad matching: guess " + binaryGuess 
						+ " correct " + binaryCorrect);
				System.exit(-1);
			}
		}
	}
	
	public void printStats() throws IOException {
		String stats = "Binary Summary\n-----\n" +
				"     TP: " + truePositives + "\n" +
				"     FP: " + falsePositives + "\n" +
				"     TN: " + trueNegatives + "\n" +
				"     FN: " + falseNegatives + "\n" +
				"  Total: " + total + "\n" +
				"     Pr: " + (truePositives / 
						(truePositives + falsePositives)) + "\n" +
				"     Re: " + (truePositives / 
						(truePositives + falseNegatives)) + "\n" +
				"     F1: " + ((2 * truePositives) / 
						(2 * truePositives + falsePositives + falseNegatives)) + "\n\n" +
				"     Gold matches: " + goldMatches + "\n" +
				"  Gold mismatches: " + goldMismatches + "\n" +
				"   Silver matches: " + silverMatches + "\n" +
				"Silver mismatches: " + silverMismatches + "\n";
		
		outWriter.write(stats);
		outWriter.close();
		System.out.println(stats);
	}
}
