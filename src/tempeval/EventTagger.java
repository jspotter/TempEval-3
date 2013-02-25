package tempeval;

import java.io.*;
import java.util.*;

import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.*;

public class EventTagger {

	/*
	 * Adds event annotations to annotation object by stripping out EVENT
	 * tags and adding their information to the tokens they contain. Also
	 * strips out TIMEX3 and SIGNAL tags.
	 */
	public static void annotate(Annotation annotation) {
		// Printing results
		PrintWriter out = new PrintWriter(System.out);
		
		// Keep track of current event type
		String curEventType = "O";
		String curEventId = "";

		// Add event information to each event-tagged token
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			
			// Keep track of tokens to remove
			Set<CoreLabel> tokensToRemove = new HashSet<CoreLabel>();
			
			// Annotate each token
			for (CoreLabel token: tokens) {				
				String word = token.get(TextAnnotation.class);
				
				// Handle EVENT start tag
				if (word.startsWith("<EVENT")) {
					
					// Extract class and event ID
					int start = word.indexOf("class=\"") + 7;
					String intermediate = word.substring(start);
					int end = intermediate.indexOf("\"");
					curEventType = intermediate.substring(0, end);
					
					start = word.indexOf("eid=\"") + 7;
					intermediate = word.substring(start);
					end = intermediate.indexOf("\"");
					curEventId = intermediate.substring(0, end);
					
					tokensToRemove.add(token);
					
				// Handle EVENT end tag
				} else if (word.startsWith("</EVENT")) {
					curEventType = "O";
					tokensToRemove.add(token);
					
				// Handle other tags (skip)
				} else if (word.contains("<TIMEX3") 
						|| word.contains("TIMEX3>")
						|| word.contains("<SIGNAL")
						|| word.contains("SIGNAL>")) {
					tokensToRemove.add(token);
					
				// Handle non-tag tokens
				} else {
					token.set(EventClassAnnotation.class, curEventType);
					token.set(EventIdAnnotation.class, curEventId);
				}
			}
			
			// Remove tokens corresponding to tags
			for (CoreLabel token: tokensToRemove) {
				tokens.remove(token);
			}
		}
	}
	
	/*
	 * Prints event annotations in two-column format.
	 */
	public static void printAnnotations(Annotation annotation, BufferedWriter out) throws IOException {
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				String word = token.getString(TextAnnotation.class);
				String event = token.getString(EventClassAnnotation.class);
				
				out.write(word + " ");
				if(event != null)
					out.write(event);
				else
					out.write("O");
				out.write("\n");
				out.flush();
			}
		}
	}

}
