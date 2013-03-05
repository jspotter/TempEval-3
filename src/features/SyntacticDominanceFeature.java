package features;

import java.util.List;

import annotationclasses.AuxTokenInfoAnnotation;

import dataclasses.AuxTokenInfo;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class SyntacticDominanceFeature implements TempEvalFeature {

	@Override
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		AuxTokenInfo auxInfo1 = token1.get(AuxTokenInfoAnnotation.class);
		AuxTokenInfo auxInfo2 = token2.get(AuxTokenInfoAnnotation.class);
		
		//TODO this
	}

}
