package features;

import java.util.List;

import annotationclasses.AuxTokenInfoAnnotation;
import dataclasses.AuxTokenInfo;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.ling.HasIndex;

public class SyntacticRelationFeature implements TempEvalFeature {
	
	private static final String SYN_REL_STRING = "__SYN_REL__";
	private static final String TREE_DIST_STRING = "__TREE_DIST__";
	
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
	
	/*
	 * Gets a distance bucket string based on distance
	 */
	private static String getDistanceBucket(int distance) {
		if (distance == 0)
			return "0";
		if (distance <= 5)
			return "<=5";
		if (distance <= 9)
			return "6-9";
		if (distance <= 13)
			return "10-13";
		return ">13";
	}
	
	@Override
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		AuxTokenInfo aux1 = token1.get(AuxTokenInfoAnnotation.class); // aux token info for timex
		AuxTokenInfo aux2 = token2.get(AuxTokenInfoAnnotation.class); // aux token info for event
		assert (aux1.tree.equals(aux2.tree));

		Tree t1 = getLeaf(aux1.tree, aux1.tree_idx);
		Tree t2 = getLeaf(aux2.tree, aux2.tree_idx);
		Tree lowest_subsumer = aux1.tree.joinNode(t1, t2);
		if (lowest_subsumer == null) System.out.println("No Subsumer!");
		else {
			String synRel;
			String type = lowest_subsumer.toString().split(" ")[0];
			if (type.equals("(NP") || type.equals("(VP") || type.equals("(PP")) {
				synRel = "SAME_PHRASE";
			} else if (type.equals("(SBAR")) {
				synRel = "SAME_SUBORDINATE CLAUSE";
			} else if (type.equals("(SINV")) {
				synRel = "SAME_INVERTED_CLAUSE";
			} else if (type.equals("(S")){
				synRel = "SAME_SENTENCE"; 
			} else {
				synRel = "OTHER";
			}
			//System.out.println(lowest_subsumer.toString());
			//System.out.println(t1.toString());
			//System.out.println(t2.toString());
			
			features.add(SYN_REL_STRING + "=" + synRel);
			
			/* add tree distance as a feature */
			int d1 = lowest_subsumer.pathNodeToNode(lowest_subsumer, t1).size();
			int d2 = lowest_subsumer.pathNodeToNode(lowest_subsumer, t2).size();
			
			List<Tree> t3 = aux1.tree.pathNodeToNode(t1, t2);
			
			int dist = t3 != null ? t3.size() : d1 + d2;
			features.add(TREE_DIST_STRING + "=" + getDistanceBucket(dist));
			
			//features.add(SYN_REL_STRING + TREE_DIST_STRING + "=" + synRel + "_" + getDistanceBucket(dist));
		}
	}
}
