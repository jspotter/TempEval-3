package features;

import java.util.List;

import annotationclasses.TimeAnnotation;
import dataclasses.TimeInfo;

import edu.stanford.nlp.ling.CoreLabel;

public class TimexTypeFeature implements TempEvalFeature {
	
	private static final String TIME_TYPE_STRING = "__TIMETYPE__";

	@Override
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		TimeInfo timeInfo = token1.get(TimeAnnotation.class);
		features.add(TIME_TYPE_STRING + "=" + timeInfo.currTimeType);
	}
}
