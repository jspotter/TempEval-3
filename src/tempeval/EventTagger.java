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

	public static void annotate(Annotation annotation) {
		// Printing results
		PrintWriter out = new PrintWriter(System.out);
		
		// Keep track of current event type
		String curEventType = "O";

		// Add event information to each event-tagged token
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			
			// Keep track of tokens to remove
			Set<CoreLabel> tokensToRemove = new HashSet<CoreLabel>();
			
			// Annotate each token
			for (CoreLabel token: tokens) {				
				String word = token.get(TextAnnotation.class);
				if (word.startsWith("<EVENT")) {
					int start = word.indexOf("class=\"") + 7;
					String intermediate = word.substring(start);
					int end = intermediate.indexOf("\"");
					curEventType = intermediate.substring(0, end);
					tokensToRemove.add(token);
				} else if (word.startsWith("</EVENT")) {
					curEventType = "O";
					tokensToRemove.add(token);;
				} else if (word.contains("<TIMEX3") 
						|| word.contains("TIMEX3>")
						|| word.contains("<SIGNAL")
						|| word.contains("SIGNAL>")) {
					tokensToRemove.add(token);;
				} else {
					token.set(EventClassAnnotation.class, curEventType);
				}
			}
			
			// Remove tokens corresponding to tags
			for (CoreLabel token: tokensToRemove) {
				tokens.remove(token);
			}
		}
	}
	
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
