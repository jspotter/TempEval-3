package features;

import java.util.List;

import annotationclasses.AuxTokenInfoAnnotation;
import dataclasses.AuxTokenInfo;

import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.ling.HasIndex;

public class HeadingPrepFeature implements TempEvalFeature {
	
	private static final String HEAD_PREP_STRING = "__HEAD_PREP__";
	
	private Tree getLeaf(Tree tree, int idx) {
		if (tree.isPreTerminal()) {
			if (((HasIndex)tree.firstChild().label()).index() == idx) return tree;
			return null;
		}
		for (Tree child : tree.children()) {
			Tree leaf = getLeaf(child, idx);
			if (leaf != null) return leaf;
		}
		return null;
	}
	
	@Override
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		AuxTokenInfo aux = token1.get(AuxTokenInfoAnnotation.class);

		Tree t = getLeaf(aux.tree, aux.tree_idx);
	}
}
