package features;

import java.util.List;

import annotationclasses.AuxTokenInfoAnnotation;
import annotationclasses.EventAnnotation;

import dataclasses.AuxTokenInfo;
import dataclasses.EventInfo;

import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;

public class WindowFeature implements TempEvalFeature {
	
	private static final String LEFT_OF_FIRST = "__LEFT_OF_FIRST__";
	private static final String RIGHT_OF_FIRST = "__RIGHT_OF_FIRST__";
	private static final String LEFT_OF_SECOND = "__LEFT_OF_SECOND__";
	private static final String RIGHT_OF_SECOND = "__RIGHT_OF_SECOND__";
	private static final String NEAR_FIRST = "__NEAR_FIRST__";
	private static final String NEAR_SECOND = "__NEAR_SECOND__";
	private static final String NEAR_SOMEONE = "__NEAR_SOMEONE__";
	
	private static int nextID = 0;
	private int id;
	
	/*
	 * Number of words examined in either direction.
	 */
	private int windowSize = 0;
	private Class infoClass = TextAnnotation.class;
	
	public WindowFeature(int size, Class infoClass) {
		windowSize = size;
		this.infoClass = infoClass;
		this.id = nextID++;
	}
	
	public void setWindowSize(int size) {
		windowSize = size;
	}
	
	public int getWindowSize() {
		return windowSize;
	}

	@Override
	//If only finding window size around one token, pass token in as token1 and pass null in for token2
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		
		AuxTokenInfo aux1 = token1.get(AuxTokenInfoAnnotation.class);
		CoreLabel left1 = aux1.prev;
		CoreLabel right1 = aux1.next;
		CoreLabel left2 = null;
		CoreLabel right2 = null;
		
		if (token2 != null){
			 AuxTokenInfo aux2 = token2.get(AuxTokenInfoAnnotation.class);
			 left2 = aux2.prev;
			 right2 = aux2.next;
		}
		
		for (int i = 0; i < windowSize; i++) {
			if (left1 != null) {
				String value = left1.get(infoClass);
				features.add(LEFT_OF_FIRST + id + value + "=TRUE");
				features.add(NEAR_FIRST + id + value + "=TRUE");
				features.add(NEAR_SOMEONE + id + value + "=TRUE");
				left1 = left1.get(AuxTokenInfoAnnotation.class).prev;
			}
			
			if (right1 != null) {
				String value = right1.get(infoClass);
				features.add(RIGHT_OF_FIRST + id + value + "=TRUE");
				features.add(NEAR_FIRST + id + value + "=TRUE");
				features.add(NEAR_SOMEONE + id + value + "=TRUE");
				right1 = right1.get(AuxTokenInfoAnnotation.class).next;
			}
			
			if (left2 != null) {
				String value = left2.get(infoClass);
				features.add(LEFT_OF_SECOND + id + value + "=TRUE");
				features.add(NEAR_SECOND + id + value + "=TRUE");
				features.add(NEAR_SOMEONE + id + value + "=TRUE");
				left2 = left2.get(AuxTokenInfoAnnotation.class).prev;
			}
			
			if (right2 != null) {
				String value = right2.get(infoClass);
				features.add(RIGHT_OF_SECOND + id + value + "=TRUE");
				features.add(NEAR_SECOND + id + value + "=TRUE");
				features.add(NEAR_SOMEONE + id + value + "=TRUE");
				right2 = right2.get(AuxTokenInfoAnnotation.class).next;
			}
		}
	}

}
