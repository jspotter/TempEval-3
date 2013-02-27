package tempeval;

import edu.stanford.nlp.ling.CoreAnnotation;

public class TokenOffsetAnnotation implements CoreAnnotation<Integer> {

	@Override
	public Class<Integer> getType() {
		return Integer.class;
	}
	
}
