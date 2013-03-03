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
import features.DistanceFeature;
import features.EventTypeFeature;
import features.InterleavingCommaFeature;
import features.TimexTypeFeature;

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

public class TimexEventTagger {

	private static final String CLASSIFIER_FILENAME = "classifiers/timex-event-model.ser.gz";

	private LinearClassifier<String, String> trainClassifier, testClassifier;
	private LinearClassifierFactory<String, String> factory;
	private List<Datum<String, String>> trainingData;
	private DistanceFeature distance;
	private InterleavingCommaFeature comma;
	private TimexTypeFeature timex;

	public TimexEventTagger() {
		factory = new LinearClassifierFactory<String, String>();
		factory.useConjugateGradientAscent();
		factory.setVerbose(true);
		factory.setSigma(10.0);

		trainingData = new ArrayList<Datum<String, String>>();
		
		distance = new DistanceFeature();
		comma = new InterleavingCommaFeature();
		timex = new TimexTypeFeature();
	}

	/*
	 * Get all same-sentence event-timex pairs for a document. First element of each pair
	 * is a timex. Second is an event.
	 */
	private static Set<Pair<CoreLabel, CoreLabel>> getEventTimexPairs(Annotation annotation) {
		Set<Pair<CoreLabel, CoreLabel>> result = new HashSet<Pair<CoreLabel, CoreLabel>>();
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

		// For each sentence, find all timexes and events, and then make pairs
		for(CoreMap sentence: sentences) {

			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);

			// Keep track of all events and timexes in this sentence
			Set<CoreLabel> timexes = new HashSet<CoreLabel>();
			Set<CoreLabel> events = new HashSet<CoreLabel>();

			// Keep track of last eid and tid (for multi-token timexes and events)
			String lastEid = null;
			String lastTid = null;

			// Iterate through tokens and store all events and timexes. Relies on the
			// invariant that no token will be both an event and a timex.
			for (CoreLabel token: tokens) {
				EventInfo eventInfo = token.get(EventAnnotation.class);
				TimeInfo timeInfo = token.get(TimeAnnotation.class);

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

				// Handle time tokens
				if (timeInfo != null) {

					// Check for multiple tokens in same time tag
					if (lastTid != null && lastTid.equals(timeInfo.currTimeId))
						continue;
					lastTid = timeInfo.currTimeId;

					timexes.add(token);
				} else {
					lastTid = null;
				}
			}

			// Make pairs for this sentence
			for (CoreLabel timex: timexes) {
				for (CoreLabel event: events) {
					result.add(new Pair<CoreLabel, CoreLabel>(timex, event));
				}
			}
		}

		return result;
	}

	/*
	 * Get true same-sentence relationships between timexes and events from
	 * the training data. Key string is tid of timex. Second key string is
	 * eiid of event. Final value is relationship type.
	 */
	private static Map<String, Map<String, String>> getEventTimexRelationships(Document doc) {
		Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();

		Element root = doc.getDocumentElement();
		Element[] tlinkElems = 
				XMLParser.getElementsByTagNameNR(root, "TLINK");
		for(Element e : tlinkElems) {
			String timeID = e.getAttribute("timeID");
			String eventInstanceID = e.getAttribute("eventInstanceID");
			String relatedToTime = e.getAttribute("relatedToTime");
			String relatedToEventInstance = e.getAttribute("relatedToEventInstance");
			String relType = e.getAttribute("relType");

			if (timeID.length() > 0 && relatedToEventInstance.length() > 0
					&& relType.length() > 0) {
				MapUtils.doublePut(result, timeID, relatedToEventInstance, relType);
			} else if (eventInstanceID.length() > 0 && relatedToTime.length() > 0
					&& relType.length() > 0) {
				MapUtils.doublePut(result, relatedToTime, eventInstanceID, relType);
			}
		}

		return result;
	}

	/*
	 * Creates a single datum from a training example
	 */
	private Datum<String, String> getDatum(CoreLabel timeToken, CoreLabel eventToken,
			Set<Pair<CoreLabel, CoreLabel>> pairs, Map<String, Map<String, String>> relationships) {

		List<String> features = new ArrayList<String>();

		TimeInfo timeInfo = timeToken.get(TimeAnnotation.class);
		EventInfo eventInfo = eventToken.get(EventAnnotation.class);

		// FEATURES
		distance.add(features, timeToken, eventToken);
		comma.add(features, timeToken, eventToken);
		timex.add(features, timeToken, null);

		// LABEL
		String label = MapUtils.doubleGet(relationships, timeInfo.currTimeId, eventInfo.currEiid);
		label = (label == null ? "O" : label);
		//System.out.println(label);

		return new BasicDatum<String, String>(features, label);
	}

	/*
	 * Train classifier on relationships between same-sentence timexes and events.
	 */
	public void train(Annotation annotation, Document doc) {
		// Find all possible same-sentence timex event pairs
		// Extract JUST the links between events and timex's from parsed XML (doc)
		Set<Pair<CoreLabel, CoreLabel>> pairs = getEventTimexPairs(annotation);
		Map<String, Map<String, String>> relationships = getEventTimexRelationships(doc);

		for (Pair<CoreLabel, CoreLabel> pair: pairs) {
			trainingData.add(getDatum(pair.first, pair.second, pairs, relationships));
		}
	}

	/*
	 * Zips classifier into file
	 */
	public void doneClassifying() {
		trainClassifier = factory.trainClassifier(trainingData);
		LinearClassifier.writeClassifier(trainClassifier, CLASSIFIER_FILENAME);
	}

	/*
	 * Loads test classifier from file (presumably written from training)
	 */
	public void loadTestClassifier() {
		testClassifier = LinearClassifier.readClassifier(CLASSIFIER_FILENAME);
	}

	/*
	 * Tests classifier
	 */
	public void test(Annotation annotation, Document doc) {
		DocInfo docInfo = annotation.get(DocInfoAnnotation.class);

		// Find all possible same-sentence timex event pairs
		// Extract JUST the links between events and timex's from parsed XML (doc)
		Set<Pair<CoreLabel, CoreLabel>> pairs = getEventTimexPairs(annotation);
		Map<String, Map<String, String>> relationships = getEventTimexRelationships(doc);

		for (Pair<CoreLabel, CoreLabel> pair: pairs) {
			Datum<String, String> datum = getDatum(pair.first, pair.second, pairs, relationships);
			String guess = testClassifier.classOf(datum);
			String correct = datum.label();

			System.out.println(pair.first.get(TimeAnnotation.class).currTimeId + " "
					+ pair.second.get(EventAnnotation.class).currEiid + " "
					+ guess + " " + correct);
			testClassifier.justificationOf(datum);

			if (!guess.equals("O")) {
				System.out.println("guessed something in file " + docInfo.filename);
				TimeInfo timeInfo = pair.first.get(TimeAnnotation.class);
				EventInfo eventInfo = pair.second.get(EventAnnotation.class);
				LinkInfo link = new LinkInfo("-1", guess, timeInfo,
						null, eventInfo);
				pair.first.set(LinkInfoAnnotation.class, link);
			}
		}
	}
}
