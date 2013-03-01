package tempeval;

import edu.stanford.nlp.ling.CoreAnnotation;

public class LinkInfoAnnotation implements CoreAnnotation<LinkInfo> {

	@Override
	public Class<LinkInfo> getType() {
		return LinkInfo.class;
	}

}
