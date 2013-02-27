package tempeval;

import edu.stanford.nlp.classify.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

import org.w3c.dom.*;

import java.io.*;
import java.util.*;

public class EventRelTagger {

	private static LinearClassifier<String, String> classifier;

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
				System.out.println(token.get(TextAnnotation.class));
				EventTagger.EventInfo eventInfo = token.get(EventAnnotation.class);
				EventTagger.TimeInfo timeInfo = token.get(TimeAnnotation.class);

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

			if (timeID != null && relatedToEventInstance != null && relType != null) {
				MapUtils.doublePut(result, timeID, relatedToEventInstance, relType);
			} else if (eventInstanceID != null && relatedToTime != null && relType != null) {
				MapUtils.doublePut(result, relatedToTime, eventInstanceID, relType);
			}
		}

		return result;
	}

	public static void trainEventTimex(Annotation annotation, Document doc) {
		//TODO extract JUST the links between events and timex's from parsed XML (doc)
		//TODO use extracted information to annotate event tokens with relationships to 
		//     timex's in the SAME SENTENCE ONLY
	}

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
			EventTagger.TimeInfo timeInfo = pair.first.get(TimeAnnotation.class);
			EventTagger.EventInfo eventInfo = pair.second.get(EventAnnotation.class);
			
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
