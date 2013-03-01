package annotationclasses;

import dataclasses.DocInfo;
import edu.stanford.nlp.ling.CoreAnnotation;

public class DocInfoAnnotation implements CoreAnnotation<DocInfo> {

	@Override
	public Class<DocInfo> getType() {
		return DocInfo.class;
	}

}
