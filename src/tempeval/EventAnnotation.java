package tempeval;

import edu.stanford.nlp.ling.CoreAnnotation;

public class EventAnnotation implements CoreAnnotation<EventTagger.EventInfo> {

	@Override
	public Class<EventTagger.EventInfo> getType() {
		return EventTagger.EventInfo.class;
	}
}
