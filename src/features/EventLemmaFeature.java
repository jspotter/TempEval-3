package features;

import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreLabel;

public class EventLemmaFeature implements TempEvalFeature {
	
	private static final String FIRST_EVENT_LEMMA = "__FIRST_EVENT_LEMMA__";
	private static final String SECOND_EVENT_LEMMA = "__SECOND_EVENT_LEMMA__";
	private static final String EVENT_LEMMA_MATCH = "__EVENT_LEMMA_MATCH__";

	@Override
	//If only run on a single token, pass in token as token1 and null for token2
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		String lemma1 = token1.get(LemmaAnnotation.class);		
		features.add(FIRST_EVENT_LEMMA + "=" + lemma1);
		
		if(token2 != null){
			String lemma2 = token2.get(LemmaAnnotation.class);
			features.add(SECOND_EVENT_LEMMA + "=" + lemma2);
			if (lemma2.equals(lemma1))
				features.add(EVENT_LEMMA_MATCH + "=TRUE");
		}
	}

}
