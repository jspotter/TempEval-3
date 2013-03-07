package helperclasses;

import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.HasIndex;
import edu.stanford.nlp.trees.Tree;

public class TreeUtils {
	public static Tree getLeaf(Tree tree, int idx) {
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
	 * Gets the lowest ancestor node having any of the given POSs,
	 * except if any of the given bad POSs are in between.
	 */
	public static Tree getLowestPOS(Tree root, Tree leaf, String[] goodPOS,
			String[] badPOS) {
		if (root == null || leaf == null || goodPOS == null || badPOS == null)
			return null;
		
		List<Tree> path = root.pathNodeToNode(root, leaf);
		for (int i = path.size() - 1; i >= 0; i--) {
			Tree cur = path.get(i);
			for (String pos : goodPOS) {
				if (cur.toString().length() >= pos.length() 
						&& cur.toString().substring(0, pos.length()).equals(pos)) {
					return cur;
				}
			}
			
			for (String pos : badPOS) {
				if (cur.toString().length() >= pos.length() 
						&& cur.toString().substring(0, pos.length()).equals(pos)) {
					return null;
				}
			}
		}

		return null;
	}
}
