package tempeval;

import helperclasses.XMLParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import annotationclasses.DocInfoAnnotation;
import annotationclasses.EventAnnotation;
import annotationclasses.LinkInfoAnnotation;
import dataclasses.DocInfo;
import dataclasses.EventInfo;
import dataclasses.LinkInfo;
import dataclasses.TimeInfo;

import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
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

public class DCTEventTagger {

	private static final String CLASSIFIER_FILENAME = "classifiers/DCT-event-model.ser.gz";

	private LinearClassifier<String, String> trainClassifier, testClassifier;
	private LinearClassifierFactory<String, String> factory;
	private List<Datum<String, String>> trainingData;
	private List<TempEvalFeature> featureList;

	public DCTEventTagger() {
		factory = new LinearClassifierFactory<String, String>();
		factory.useConjugateGradientAscent();
		factory.setVerbose(true);
		factory.setSigma(10.0);

		trainingData = new ArrayList<Datum<String, String>>();
		featureList = new ArrayList<TempEvalFeature>();
		
		//featureList.add(new DistanceFeature());
		//featureList.add(new InterleavingCommaFeature());
		featureList.add(new WindowFeature(3, TextAnnotation.class));
		featureList.add(new EventTypeFeature());
		featureList.add(new POSFeature());
		featureList.add(new EventLemmaFeature());
		featureList.add(new WindowFeature(2, PartOfSpeechAnnotation.class));
		
		//TODO declare features here
	}

	/*
	 * Get all document creation time - event pairs.
	 */
	private static Set<Pair<String, CoreLabel>> getDCTEventPairs(Annotation annotation, TimeInfo dct) {
		Set<Pair<String, CoreLabel>> result = new HashSet<Pair<String, CoreLabel>>();
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

		for(CoreMap sentence: sentences) {
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			String lastEiid = null;
			for (CoreLabel token: tokens) {
				EventInfo eventInfo = token.get(EventAnnotation.class);
				if (eventInfo != null) {
					if (lastEiid == null || !lastEiid.equals(eventInfo.currEventId)){
						lastEiid = eventInfo.currEventId;
						result.add(new Pair<String, CoreLabel>(dct.currTimeId,token));
					} else {
						lastEiid = null;
					}
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
	private static Map<String, String> getDCTEventRelationships(Document doc, TimeInfo dctTimeInfo) {
		Map<String, String> result = new HashMap<String, String>();
		
		Element root = doc.getDocumentElement();
		Element[] tlinkElems = 
				XMLParser.getElementsByTagNameNR(root, "TLINK");
		for(Element e : tlinkElems) {
			String eventInstanceID = e.getAttribute("eventInstanceID");
			String relatedToTime = e.getAttribute("relatedToTime");
			String relType = e.getAttribute("relType");
			if(relatedToTime != null && relatedToTime.equals(dctTimeInfo.currTimeId)){
				result.put(eventInstanceID, relType);
			}
		}
		return result;
	}

	/*
	 * Creates a single datum from a training example
	 */
	private Datum<String, String> getDatum(String first, CoreLabel eventToken,
			Set<Pair<String, CoreLabel>> pairs, Map<String, String> relationships) {

		List<String> features = new ArrayList<String>();
		EventInfo eventInfo = eventToken.get(EventAnnotation.class);

		// FEATURES
		//TODO: add features to features List

		// LABEL
		String label = relationships.get(eventInfo.currEiid);
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
		
		/*
		TimeInfo dctTimeInfo = annotation.get(DocInfoAnnotation.class).dctTimeInfo;
		Set<Pair<String, CoreLabel>> pairs = getDCTEventPairs(annotation, dctTimeInfo);
		Map<String, String> relationships = getDCTEventRelationships(doc, dctTimeInfo);

		for (Pair<String, CoreLabel> pair: pairs) {
			trainingData.add(getDatum(pair.first, pair.second, pairs, relationships));
		}
		*/
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
		/*
		TimeInfo dctTimeInfo = annotation.get(DocInfoAnnotation.class).dctTimeInfo;
		Set<Pair<String, CoreLabel>> pairs = getDCTEventPairs(annotation, dctTimeInfo);
		Map<String, String> relationships = getDCTEventRelationships(doc, dctTimeInfo);

		for (Pair<String, CoreLabel> pair: pairs) {
			Datum<String, String> datum = getDatum(pair.first, pair.second, pairs, relationships);
			String guess = testClassifier.classOf(datum);
			//String correct = datum.label();

			
			testClassifier.justificationOf(datum);

			if (!guess.equals("O")) {
				System.out.println("guessed something in file " + docInfo.filename);
				EventInfo eventInfo = pair.second.get(EventAnnotation.class);
				LinkInfo link = new LinkInfo("-1", guess, dctTimeInfo,
						null, eventInfo);
				pair.second.set(LinkInfoAnnotation.class, link);
			}
			
		}
		*/
	}
}
