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

	public static void train(StanfordCoreNLP pipeline, Annotation annotation,
			BufferedWriter trainwriter, BufferedWriter testwriter) {
		// Printing results
		PrintWriter out = new PrintWriter(System.out);
		PrintWriter xmlOut = null;
		
		// Initial event ID
		int eventID = 1;
		
		// Keep track of current event type
		String curEventType = "O";

		// An Annotation is a Map and you can get and use the various analyses individually.
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				// this is the text of the token
				//String word = token.get(TextAnnotation.class);
				// this is the POS tag of the token
				//String pos = token.get(PartOfSpeechAnnotation.class);
				// this is the NER label of the token
				//String ne = token.get(NamedEntityTagAnnotation.class);

				//Timex timex = token.get(TimexAnnotation.class);

				//System.out.println(word + " : " + pos + " : " + ne);
				//if (timex != null)
				//	System.out.println(timex.toString());
				
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
					try {
						trainwriter.append(word + " " + curEventType + "\n");
						testwriter.append(word + "\n");
					} catch(IOException e) {
						e.printStackTrace();
						System.exit(-1);
					}
				}
			}
		}
		
		// Print
		//pipeline.prettyPrint(annotation, out);
	}

}
