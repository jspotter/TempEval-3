package features;

import java.util.List;

import edu.stanford.nlp.ling.CoreLabel;

public interface TempEvalFeature {

	public void add(List<String> features, CoreLabel token1, CoreLabel token2);
}
