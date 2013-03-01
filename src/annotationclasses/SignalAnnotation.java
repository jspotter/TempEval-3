package annotationclasses;

import edu.stanford.nlp.ling.CoreAnnotation;

public class SignalAnnotation implements CoreAnnotation<Boolean> {

	@Override
	public Class<Boolean> getType() {
		return Boolean.class;
	}

}
