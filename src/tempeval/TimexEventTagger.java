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

public class TimexEventTagger {

	private static final String CLASSIFIER_FILENAME = "classifiers/timex-event-model.ser.gz";

	private static final String DISTANCE_STRING = "__DIST__";
	private static final String INTERLEAVING_COMMA_STRING = "__COMMA__";
	private static final String TIME_TYPE_STRING = "__TIMETYPE__";
	private static final String EVENT_TYPE_STRING = "__EVENTTYPE__";

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
	 * Gets a distance bucket string based on distance
	 */
	private static String getDistanceBucket(int distance) {
		if (distance == 0)
			return "0";
		if (distance <= 1)
			return "1";
		if (distance <= 3)
			return "2-3";
		if (distance <= 5)
			return "4-5";
		return ">5";
	}

	/*
	 * Minimum window feature for timex-event tagging
	 */
	private static void addDistanceFeature(List<String> features, CoreLabel timeToken,
			CoreLabel eventToken) {
		TimeInfo timeInfo = timeToken.get(TimeAnnotation.class);
		EventInfo eventInfo = eventToken.get(EventAnnotation.class);
		AuxTokenInfo auxTimeInfo = timeToken.get(AuxTokenInfoAnnotation.class);
		AuxTokenInfo auxEventInfo = eventToken.get(AuxTokenInfoAnnotation.class);

		int timeOffset = auxTimeInfo.tokenOffset;
		int eventOffset = auxEventInfo.tokenOffset;

		//System.out.println(timeInfo.currTimeId + " " + timeOffset + " "
		//		+ eventInfo.currEiid + " " + eventOffset);

		int distance;
		if (timeOffset < eventOffset)
			distance = eventOffset - timeOffset - timeInfo.numTokens;
		else
			distance = timeOffset - eventOffset - timeInfo.numTokens;
		features.add(DISTANCE_STRING + "=" + getDistanceBucket(distance));
		//System.out.print(distance + " ");
	}

	/*
	 * Interleaving words feature for timex-event tagging
	 */
	private static void addInterleavingWordsFeature(List<String> features, CoreLabel timeToken,
			CoreLabel eventToken) {
		TimeInfo timeInfo = timeToken.get(TimeAnnotation.class);
		EventInfo eventInfo = eventToken.get(EventAnnotation.class);

		//TODO this
	}

	/*
	 * Interleaving commma feature for timex-event tagging
	 */
	private static void addInterleavingCommaFeature(List<String> features, CoreLabel timeToken,
			CoreLabel eventToken) {
		TimeInfo timeInfo = timeToken.get(TimeAnnotation.class);
		EventInfo eventInfo = eventToken.get(EventAnnotation.class);
		AuxTokenInfo auxTimeInfo = timeToken.get(AuxTokenInfoAnnotation.class);
		AuxTokenInfo auxEventInfo = eventToken.get(AuxTokenInfoAnnotation.class);

		CoreLabel next;
		AuxTokenInfo cur = auxTimeInfo;
		while(true) {
			next = cur.next;
			if (next == null || next.get(TimeAnnotation.class) == null)
				break;
			cur = next.get(AuxTokenInfoAnnotation.class);
		}

		int timeOffset = auxTimeInfo.tokenOffset;
		int eventOffset = auxEventInfo.tokenOffset;

		if (timeOffset < eventOffset && next != null 
				&& next.get(TextAnnotation.class).equals(",")) {
			features.add(INTERLEAVING_COMMA_STRING + "=TRUE");
		}
	}

	private static void addTimexTypeFeature(List<String> features, CoreLabel timeToken) {
		TimeInfo timeInfo = timeToken.get(TimeAnnotation.class);
		features.add(TIME_TYPE_STRING + "=" + timeInfo.currTimeType);
	}

	private static void addEventTypeFeature(List<String> features, CoreLabel eventToken) {
		EventInfo eventInfo = eventToken.get(EventAnnotation.class);
		features.add(EVENT_TYPE_STRING + "=" + eventInfo.currEventType);
	}

	/*
	 * Creates a single datum from a training example
	 */
	private static Datum<String, String> getDatum(CoreLabel timeToken, CoreLabel eventToken,
			Set<Pair<CoreLabel, CoreLabel>> pairs, Map<String, Map<String, String>> relationships) {

		List<String> features = new ArrayList<String>();

		TimeInfo timeInfo = timeToken.get(TimeAnnotation.class);
		EventInfo eventInfo = eventToken.get(EventAnnotation.class);

		// FEATURES
		addDistanceFeature(features, timeToken, eventToken);
		//addInterleavingWordsFeature(features, timeToken, eventToken);
		addInterleavingCommaFeature(features, timeToken, eventToken);
		addTimexTypeFeature(features, timeToken);
		//addEventTypeFeature(features, eventToken);

		// LABEL
		String label = MapUtils.doubleGet(relationships, timeInfo.currTimeId, eventInfo.currEiid);
		label = (label == null ? "O" : label);
		//System.out.println(label);

		return new BasicDatum<String, String>(features, label);
	}

	/*
	 * Train classifier on relationships between same-sentence timexes and events.
	 */
	public static void trainEventTimex(Annotation annotation, Document doc) {
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
	public static void doneClassifying() {
		trainClassifier = factory.trainClassifier(trainingData);
		LinearClassifier.writeClassifier(trainClassifier, CLASSIFIER_FILENAME);
	}

	/*
	 * Loads test classifier from file (presumably written from training)
	 */
	public static void loadTestClassifier() {
		testClassifier = LinearClassifier.readClassifier(CLASSIFIER_FILENAME);
	}

	/*
	 * Tests classifier
	 */
	public static void testEventTimex(Annotation annotation, Document doc) {

		int nextLinkID = 0;
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
				int id = nextLinkID++;
				TimeInfo timeInfo = pair.first.get(TimeAnnotation.class);
				EventInfo eventInfo = pair.second.get(EventAnnotation.class);
				LinkInfo link = new LinkInfo("" + id, guess, timeInfo,
						null, eventInfo);
				pair.first.set(LinkInfoAnnotation.class, link);
			}
		}
	}

	/*
	 * Main program to test functionality
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Testing event-timex training functionality");
		File sampleFile = new File("data/TBAQ-cleaned/AQUAINT/APW19980807.0261.tml");
		Document doc = XMLParser.parse(sampleFile);

		// Create pipeline
		Properties props = new Properties();
		//props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		props.put("annotators", "tokenize, ssplit");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		String file_text = "";
		String curr_line;
		BufferedReader br = new BufferedReader(new FileReader(sampleFile));

		while ((curr_line = br.readLine()) != null) {
			file_text += curr_line;
		}

		br.close();

		file_text = XMLParser.getRawTextByTagName(file_text, "<TEXT>", "</TEXT>");
		System.out.println(file_text);
		Annotation annotation = new Annotation(file_text);

		pipeline.annotate(annotation);
		EventTagger.annotate(annotation, doc);

		Set<Pair<CoreLabel, CoreLabel>> pairs = getEventTimexPairs(annotation);
		Map<String, Map<String, String>> relationships = getEventTimexRelationships(doc);

		System.out.println("About to print extracted information");

		for (Pair<CoreLabel, CoreLabel> pair: pairs) {
			TimeInfo timeInfo = pair.first.get(TimeAnnotation.class);
			EventInfo eventInfo = pair.second.get(EventAnnotation.class);

			String tid = timeInfo.currTimeId;
			String eiid = eventInfo.currEiid;

			System.out.print(tid + " " + eiid + " ");

			String relType = MapUtils.doubleGet(relationships, tid, eiid);
			if (relType != null) {
				System.out.println(relType);
			} else {
				System.out.println("O");
			}
		}
	}
}
