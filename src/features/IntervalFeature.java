package features;

import java.util.List;

import annotationclasses.AuxTokenInfoAnnotation;
import dataclasses.AuxTokenInfo;

import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;

public class IntervalFeature implements TempEvalFeature {
	
	private static final String INTERVAL_STRING = "__INTER__";
	
	@Override
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		AuxTokenInfo auxTimeInfo = token1.get(AuxTokenInfoAnnotation.class);

		CoreLabel prev = auxTimeInfo.prev;
		if (prev != null) {
			//System.out.println(prev.get(TextAnnotation.class));
			String s = prev.get(TextAnnotation.class);
			if (s.equalsIgnoreCase("before")) {
				//System.out.println("before firing");
				features.add(INTERVAL_STRING + "=" + s.toLowerCase());
			} else if (s.equalsIgnoreCase("after")) {
				//System.out.println("after firing");
				features.add(INTERVAL_STRING + "=" + s.toLowerCase());
			} else if (s.equalsIgnoreCase("during")) {
				//System.out.println("during firing");
				features.add(INTERVAL_STRING + "=" + s.toLowerCase());
			} else if (s.equalsIgnoreCase("on")) {
				//System.out.println("on firing");
				features.add(INTERVAL_STRING + "=" + s.toLowerCase());
			} else if (s.equalsIgnoreCase("this")) {
				//System.out.println("this firing");
				features.add(INTERVAL_STRING + "=" + s.toLowerCase());
			}
		}
	}
}
