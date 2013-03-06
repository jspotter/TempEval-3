package features;

import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreLabel;

public class POSFeature implements TempEvalFeature {
	
	private static final String FIRST_POS = "__FIRST_POS__";
	private static final String SECOND_POS = "__SECOND_POS__";
	private static final String POS_MATCH = "__POS_MATCH__";

	@Override
	//If for single token, pass in token as token1 and null for token2
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		String pos1 = token1.get(PartOfSpeechAnnotation.class);
		features.add(FIRST_POS + "=" + pos1);

		if(token2 != null){
			String pos2 = token2.get(PartOfSpeechAnnotation.class);
			features.add(SECOND_POS + "=" + pos2);
			if (pos2.equals(pos1))
				features.add(POS_MATCH + "=TRUE");
		}	
	}

}
