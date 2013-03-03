package features;

import java.util.List;

import annotationclasses.AuxTokenInfoAnnotation;
import annotationclasses.TimeAnnotation;
import dataclasses.AuxTokenInfo;
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
		AuxTokenInfo auxTimeInfo = token1.get(AuxTokenInfoAnnotation.class);
		AuxTokenInfo auxEventInfo = token2.get(AuxTokenInfoAnnotation.class);

		int timeOffset = auxTimeInfo.tokenOffset;
		int eventOffset = auxEventInfo.tokenOffset;

		//System.out.println(timeInfo.currTimeId + " " + timeOffset + " "
		//		+ eventInfo.currEiid + " " + eventOffset);

		int distance;
		if (timeOffset < eventOffset)
			distance = eventOffset - timeOffset - timeInfo.numTokens;
		else
			distance = timeOffset - eventOffset - timeInfo.numTokens;
		features.add(DISTANCE_STRING + "=" + getDistanceBucket(distance));
		//System.out.print(distance + " ");
	}

}
