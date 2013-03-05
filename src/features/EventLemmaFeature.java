package features;

import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreLabel;

public class EventLemmaFeature implements TempEvalFeature {
	
	private static final String FIRST_EVENT_LEMMA = "__FIRST_EVENT_LEMMA__";
	private static final String SECOND_EVENT_LEMMA = "__SECOND_EVENT_LEMMA__";

	@Override
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		String lemma1 = token1.get(LemmaAnnotation.class);
		String lemma2 = token2.get(LemmaAnnotation.class);
		
		features.add(FIRST_EVENT_LEMMA + "=" + lemma1);
		features.add(SECOND_EVENT_LEMMA + "=" + lemma2);
	}

}
