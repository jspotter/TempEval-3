package features;

import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreLabel;

public class POSFeature implements TempEvalFeature {
	
	private static final String FIRST_POS = "__FIRST_POS__";
	private static final String SECOND_POS = "__SECOND_POS__";

	@Override
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		String pos1 = token1.get(PartOfSpeechAnnotation.class);
		String pos2 = token2.get(PartOfSpeechAnnotation.class);
		
		features.add(FIRST_POS + "=" + pos1);
		features.add(SECOND_POS + "=" + pos2);
	}

}
