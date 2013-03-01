package tempeval;

import edu.stanford.nlp.ling.CoreAnnotation;

public class EventAnnotation implements CoreAnnotation<EventInfo> {

	@Override
	public Class<EventInfo> getType() {
		return EventInfo.class;
	}
}
