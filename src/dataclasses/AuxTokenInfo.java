package dataclasses;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;

public class AuxTokenInfo {
	public int tokenOffset;
	public CoreLabel prev;
	public CoreLabel next;
	public Tree tree;
	public int tree_idx;
	
	public AuxTokenInfo() {
		tokenOffset = -1;
		prev = null;
		next = null;
		tree = null;
		tree_idx = -1;
	}
}
