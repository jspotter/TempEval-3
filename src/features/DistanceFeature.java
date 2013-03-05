package features;

import java.util.List;

import annotationclasses.AuxTokenInfoAnnotation;
import annotationclasses.EventAnnotation;
import annotationclasses.TimeAnnotation;
import dataclasses.AuxTokenInfo;
import dataclasses.EventInfo;
import dataclasses.TimeInfo;

import edu.stanford.nlp.ling.CoreLabel;

public class DistanceFeature implements TempEvalFeature {
	
	private static final String DISTANCE_STRING = "__DIST__";
	
	/*
	 * Gets a distance bucket string based on distance
	 */
	private static String getDistanceBucket(int distance) {
		if (distance == 0)
			return "0";
		if (distance <= 1)
			return "1";
		if (distance <= 3)
			return "2-3";
		if (distance <= 5)
			return "4-5";
		return ">5";
	}

	@Override
	public void add(List<String> features, CoreLabel token1, CoreLabel token2) {
		TimeInfo timeInfo = token1.get(TimeAnnotation.class);
		EventInfo eventInfo = token1.get(EventAnnotation.class);
		
		int firstNumTokens = 1;
		if (timeInfo != null)
			firstNumTokens = timeInfo.numTokens;
		if (eventInfo != null)
			firstNumTokens = eventInfo.numTokens;
		
		AuxTokenInfo auxInfo1 = token1.get(AuxTokenInfoAnnotation.class);
		AuxTokenInfo auxInfo2 = token2.get(AuxTokenInfoAnnotation.class);

		int offset1 = auxInfo1.tokenOffset;
		int offset2 = auxInfo2.tokenOffset;

		//System.out.println(timeInfo.currTimeId + " " + timeOffset + " "
		//		+ eventInfo.currEiid + " " + eventOffset);

		int distance;
		if (offset1 < offset2)
			distance = offset2 - offset1 - firstNumTokens;
		else
			distance = offset2 - offset1 - firstNumTokens;
		features.add(DISTANCE_STRING + "=" + getDistanceBucket(distance));
		//System.out.print(distance + " ");
	}

}
