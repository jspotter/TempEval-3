package helperclasses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import annotationclasses.EventAnnotation;
import dataclasses.EventInfo;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

public class TestUtils {
	/*
	 * Gets mappings from golden eiids to silver eiids.
	 */
	public static Map<String, String> getEiidMappings(Annotation annotation,
			Annotation goldenAnnotation) {
		
		Map<String, String> result = new HashMap<String, String>();
		
		List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
		List<CoreMap> goldenSentences = goldenAnnotation.get(SentencesAnnotation.class);
		
		for (int i = 0; i < sentences.size(); i++) {
			List<CoreLabel> sentence = sentences.get(i).get(TokensAnnotation.class);
			List<CoreLabel> goldenSentence = goldenSentences.get(i).get(TokensAnnotation.class);
			
			for (int j = 0; j < sentence.size(); j++) {
				CoreLabel token = sentence.get(j);
				CoreLabel goldenToken = goldenSentence.get(j);
				
				EventInfo info = token.get(EventAnnotation.class);
				EventInfo goldenInfo = goldenToken.get(EventAnnotation.class);
				
				if (info != null && goldenInfo != null) {
					result.put(goldenInfo.currEiid, info.currEiid);
				}
			}
		}
		
		return result;
	}
}
