package features;

import java.util.List;

import annotationclasses.EventAnnotation;
import dataclasses.EventInfo;

import edu.stanford.nlp.ling.CoreLabel;

public class EventTypeFeature implements TempEvalFeature {
	
	private static final String EVENT_TYPE_STRING = "__EVENTTYPE__";

	@Override
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		EventInfo eventInfo = token1.get(EventAnnotation.class);
		features.add(EVENT_TYPE_STRING + "=" + eventInfo.currEventType);
	}

}
