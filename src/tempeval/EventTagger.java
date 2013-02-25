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
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {				
				String word = token.get(TextAnnotation.class);
				if (word.startsWith("<EVENT")) {
					int start = word.indexOf("class=\"") + 7;
					String intermediate = word.substring(start);
					int end = intermediate.indexOf("\"");
					curEventType = intermediate.substring(0, end);
					
					continue;
				} else if (word.startsWith("</EVENT")) {
					curEventType = "O";
					continue;
				} else if (word.contains("<TIMEX3") 
						|| word.contains("TIMEX3>")
						|| word.contains("<SIGNAL")
						|| word.contains("SIGNAL>")) {
					continue;
				} else {
					token.set(EventClassAnnotation.class, curEventType);
				}
			}
		}
	}
	
	public static void printAnnotations(Annotation annotation, PrintWriter out) {
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {	
				String word = token.getString(TextAnnotation.class);
				String event = token.getString(EventClassAnnotation.class);
				
				out.print(word + " ");
				if(event != null)
					out.println(event);
				else
					out.println("O");
			}
		}
	}

}
