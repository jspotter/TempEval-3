package tempeval;

import edu.stanford.nlp.ling.CoreAnnotation;

public class EventIdAnnotation implements CoreAnnotation<String> {

	@Override
	public Class<String> getType() {
		return java.lang.String.class;
	}

}
