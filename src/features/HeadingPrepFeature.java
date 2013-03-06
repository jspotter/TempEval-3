package features;

import java.util.List;

import annotationclasses.AuxTokenInfoAnnotation;
import dataclasses.AuxTokenInfo;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.ling.HasIndex;

public class HeadingPrepFeature implements TempEvalFeature {
	
	private static final String HEAD_PREP_STRING = "__HEAD_PREP__";
	private static final int MAX_TREE_DIST = 3;
	
	private Tree getLeaf(Tree tree, int idx) {
		if (tree == null) return null;
		if (tree.isLeaf()) {
			if (((HasIndex)tree.label()).index() == idx) return tree;
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
		AuxTokenInfo aux1 = token1.get(AuxTokenInfoAnnotation.class); // aux token info for timex
		AuxTokenInfo aux2 = token2.get(AuxTokenInfoAnnotation.class); // aux token info for event
		assert (aux1.tree.equals(aux2.tree));

		Tree t = getLeaf(aux1.tree, aux1.tree_idx);
		Tree lowest_pp = null;
		if (t != null) {
			List<Tree> path = aux1.tree.pathNodeToNode(aux1.tree, t);
			for (int i = path.size() - 1; i >= 0; i--) {
				Tree cur = path.get(i);
				if (cur.toString().length() >= 3 && cur.toString().substring(0, 3).equals("(PP")) {
					lowest_pp = cur;
					//if (path.size() - i - 1 <= MAX_TREE_DIST)
					//	features.add(HEAD_PREP_STRING + "=" + lowest_pp.firstChild().firstChild().toString().toLowerCase());
					break;
				}
			}
			if (lowest_pp == null)
				return;
		} else return;
		
		
		t = getLeaf(aux2.tree, aux2.tree_idx);
		Tree lowest_vp = null;
		if (t != null) {
			List<Tree> path = aux2.tree.pathNodeToNode(aux2.tree, t);
			for (int i = path.size() - 1; i >= 0; i--) {
				Tree cur = path.get(i);
				if (cur.toString().length() >= 3 && cur.toString().substring(0, 3).equals("(VP")) {
					lowest_vp = cur;
					break;
				}
			}
			if (lowest_vp == null)
				return;
		} else return;
		
		List<Tree> path = aux1.tree.pathNodeToNode(lowest_vp, lowest_pp);
		if (path.size() > 0 && path.size() <= MAX_TREE_DIST) {
			features.add(HEAD_PREP_STRING + "=" + lowest_pp.firstChild().firstChild().toString().toLowerCase());
		}
	}
}
