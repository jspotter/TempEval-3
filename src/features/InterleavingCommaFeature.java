package features;

import java.util.List;

import annotationclasses.AuxTokenInfoAnnotation;
import annotationclasses.TimeAnnotation;
import dataclasses.AuxTokenInfo;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;

public class InterleavingCommaFeature implements TempEvalFeature {
	
	private static final String INTERLEAVING_COMMA_STRING = "__COMMA__";

	@Override
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		AuxTokenInfo auxTimeInfo = token1.get(AuxTokenInfoAnnotation.class);
		AuxTokenInfo auxEventInfo = token2.get(AuxTokenInfoAnnotation.class);

		CoreLabel next;
		AuxTokenInfo cur = auxTimeInfo;
		while(true) {
			next = cur.next;
			if (next == null || next.get(TimeAnnotation.class) == null)
				break;
			cur = next.get(AuxTokenInfoAnnotation.class);
		}

		int timeOffset = auxTimeInfo.tokenOffset;
		int eventOffset = auxEventInfo.tokenOffset;

		if (timeOffset < eventOffset && next != null 
				&& next.get(TextAnnotation.class).equals(",")) {
			features.add(INTERLEAVING_COMMA_STRING + "=TRUE");
		}
	}

}
