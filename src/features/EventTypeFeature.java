package features;

import java.util.List;

import annotationclasses.EventAnnotation;
import dataclasses.EventInfo;

import edu.stanford.nlp.ling.CoreLabel;

public class EventTypeFeature implements TempEvalFeature {
	
	private static final String EVENT_TYPE_STRING = "__EVENTTYPE__";
	private static final String FIRST_EVENT_TYPE_STRING = "__FIRST_EVENTTYPE__";
	private static final String SECOND_EVENT_TYPE_STRING = "__SECOND_EVENTTYPE__";

	@Override
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		EventInfo eventInfo1 = token1.get(EventAnnotation.class);
		if (token2 == null) {
			features.add(EVENT_TYPE_STRING + "=" + eventInfo1.currEventType);
		} else {
			EventInfo eventInfo2 = token2.get(EventAnnotation.class);
			features.add(FIRST_EVENT_TYPE_STRING + "=" + eventInfo1.currEventType);
			features.add(SECOND_EVENT_TYPE_STRING + "=" + eventInfo2.currEventType);
		}
	}
}
