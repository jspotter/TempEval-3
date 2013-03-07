package features;

import helperclasses.TreeUtils;

import java.util.ArrayList;
import java.util.List;

import annotationclasses.AuxTokenInfoAnnotation;
import annotationclasses.EventAnnotation;

import dataclasses.AuxTokenInfo;
import dataclasses.EventInfo;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class SyntacticDominanceFeature implements TempEvalFeature {
	
	public static final String SUBORDINATE = "__IS_SUBORDINATE__";
	public static final String SUBORDINATE_SIDE = "__SUBORDINATE_SIDE__";
	public static final String SUBORDINATE_TYPE = "__SUBORDINATE_TYPE__";
	public static final String LEFT = "__LEFT__";
	public static final String RIGHT = "__RIGHT__";
	public static final String SBAR = "__SBAR__";
	public static final String SENT = "__SENT__";

	@Override
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		AuxTokenInfo auxInfo1 = token1.get(AuxTokenInfoAnnotation.class);
		AuxTokenInfo auxInfo2 = token2.get(AuxTokenInfoAnnotation.class);
		
		Tree tree1 = auxInfo1.tree;
		Tree tree2 = auxInfo2.tree;
		assert (tree1.equals(tree2));
		
		int treeIdx1 = auxInfo1.tree_idx;
		int treeIdx2 = auxInfo2.tree_idx;
		
		Tree node1 = TreeUtils.getLeaf(tree1, treeIdx1);
		Tree node2 = TreeUtils.getLeaf(tree1, treeIdx2);
		
		// These are the things we look for in the tree
		String[] vpGood = new String[]{"(VP"};
		String[] vpBad = new String[]{};
		String[] sbarGood = new String[]{"(SBAR"};
		String[] sbarBad = new String[]{"(PP"};
		String[] sentGood = new String[]{"(S"};
		String[] sentBad = sbarBad;
		
		String side = RIGHT;
		Tree vp = TreeUtils.getLowestPOS(tree1, node1, vpGood, vpBad);
		Tree sbar = TreeUtils.getLowestPOS(tree1, node2, sbarGood, sbarBad);
		Tree sent = TreeUtils.getLowestPOS(tree1, node2, sentGood, sentBad);
		if (vp == null || (sbar == null && sent == null)) {
			side = LEFT;
			vp = TreeUtils.getLowestPOS(tree1, node2, vpGood, vpBad);
			sbar = TreeUtils.getLowestPOS(tree1, node1, sbarGood, sbarBad);
			sent = TreeUtils.getLowestPOS(tree1, node2, sentGood, sentBad);
		}
		
		Tree subordinate = (sbar == null ? sent : sbar);
		String type = (sbar == null ? SENT : SBAR);
		
		// Determine if the subordinate is a child of the vp
		if (vp != null && subordinate != null) {
			List<Tree> children = vp.getChildrenAsList();
			if (children.contains(subordinate)) {
				features.add(SUBORDINATE + "=TRUE");
				features.add(SUBORDINATE_SIDE + "=" + side);
				features.add(SUBORDINATE_TYPE + "=" + type);
			}
		}
	}

}
