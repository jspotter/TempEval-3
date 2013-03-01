package annotationclasses;

import dataclasses.TimeInfo;
import edu.stanford.nlp.ling.CoreAnnotation;

public class TimeAnnotation implements CoreAnnotation<TimeInfo> {

	@Override
	public Class<TimeInfo> getType() {
		return TimeInfo.class;
	}
}
