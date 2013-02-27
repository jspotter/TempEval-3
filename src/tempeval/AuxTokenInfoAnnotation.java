package tempeval;

import edu.stanford.nlp.ling.CoreAnnotation;

public class AuxTokenInfoAnnotation implements CoreAnnotation<AuxTokenInfo> {

	@Override
	public Class<AuxTokenInfo> getType() {
		return AuxTokenInfo.class;
	}

}
