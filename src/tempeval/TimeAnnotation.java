package tempeval;

import edu.stanford.nlp.ling.CoreAnnotation;

public class TimeAnnotation implements CoreAnnotation<EventTagger.TimeInfo> {

	@Override
	public Class<EventTagger.TimeInfo> getType() {
		return EventTagger.TimeInfo.class;
	}
}
