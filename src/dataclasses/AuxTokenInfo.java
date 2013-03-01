package dataclasses;

import edu.stanford.nlp.ling.CoreLabel;

public class AuxTokenInfo {
	public int tokenOffset;
	public CoreLabel prev;
	public CoreLabel next;
	
	public AuxTokenInfo() {
		tokenOffset = -1;
		prev = null;
		next = null;
	}
}
