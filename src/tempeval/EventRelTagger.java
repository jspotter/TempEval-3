package tempeval;

import edu.stanford.nlp.classify.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

import org.w3c.dom.*;

import java.util.*;

public class EventRelTagger {
	
	private static LinearClassifier<String, String> classifier;
	
	/*
	 * Get all same-sentence event-timex pairs for a document. First element of each pair
	 * is a timex. Second is an event.
	 */
	private Set<Pair<CoreLabel, CoreLabel>> getEventTimexPairs(Annotation annotation) {
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
	
	public static void trainEventTimex(Annotation annotation, Document doc) {
		//TODO extract JUST the links between events and timex's from parsed XML (doc)
		//TODO use extracted information to annotate event tokens with relationships to 
		//     timex's in the SAME SENTENCE ONLY
	}
}
