package tempeval;

import edu.stanford.nlp.pipeline.*;

import org.w3c.*;

public class EventRelTagger {
	
	public static void annotateEventTimex(Annotation annotation, org.w3c.dom.Document doc) {
		//TODO extract JUST the links between events and timex's from parsed XML (doc)
		//TODO use extracted information to annotate event tokens with relationships to 
		//     timex's in the SAME SENTENCE ONLY
	}
}
