package tempeval;

import java.util.List;

import annotationclasses.AuxTokenInfoAnnotation;
import annotationclasses.EventAnnotation;
import annotationclasses.TimeAnnotation;
import dataclasses.AuxTokenInfo;
import dataclasses.EventInfo;
import dataclasses.TimeInfo;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;

public class FeatureFactory {
	
	private static final String DISTANCE_STRING = "__DIST__";
	private static final String INTERLEAVING_COMMA_STRING = "__COMMA__";
	private static final String TIME_TYPE_STRING = "__TIMETYPE__";
	private static final String EVENT_TYPE_STRING = "__EVENTTYPE__";
	
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
	
	/*
	 * Minimum window feature for timex-event tagging
	 */
	public static void addDistanceFeature(List<String> features, CoreLabel timeToken,
			CoreLabel eventToken) {
		TimeInfo timeInfo = timeToken.get(TimeAnnotation.class);
		AuxTokenInfo auxTimeInfo = timeToken.get(AuxTokenInfoAnnotation.class);
		AuxTokenInfo auxEventInfo = eventToken.get(AuxTokenInfoAnnotation.class);

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
	
	/*
	 * Interleaving words feature for timex-event tagging
	 */
	public static void addInterleavingWordsFeature(List<String> features, CoreLabel timeToken,
			CoreLabel eventToken) {
		TimeInfo timeInfo = timeToken.get(TimeAnnotation.class);
		EventInfo eventInfo = eventToken.get(EventAnnotation.class);

		//TODO this
	}
	
	/*
	 * Interleaving commma feature for timex-event tagging
	 */
	public static void addInterleavingCommaFeature(List<String> features, CoreLabel timeToken,
			CoreLabel eventToken) {
		AuxTokenInfo auxTimeInfo = timeToken.get(AuxTokenInfoAnnotation.class);
		AuxTokenInfo auxEventInfo = eventToken.get(AuxTokenInfoAnnotation.class);

		CoreLabel next;
		AuxTokenInfo cur = auxTimeInfo;
		while(true) {
			next = cur.next;
			if (next == null || next.get(TimeAnnotation.class) == null)
				break;
			cur = next.get(AuxTokenInfoAnnotation.class);
		}

		int timeOffset = auxTimeInfo.tokenOffset;
		int eventOffset = auxEventInfo.tokenOffset;

		if (timeOffset < eventOffset && next != null 
				&& next.get(TextAnnotation.class).equals(",")) {
			features.add(INTERLEAVING_COMMA_STRING + "=TRUE");
		}
	}

	public static void addTimexTypeFeature(List<String> features, CoreLabel timeToken) {
		TimeInfo timeInfo = timeToken.get(TimeAnnotation.class);
		features.add(TIME_TYPE_STRING + "=" + timeInfo.currTimeType);
	}

	public static void addEventTypeFeature(List<String> features, CoreLabel eventToken) {
		EventInfo eventInfo = eventToken.get(EventAnnotation.class);
		features.add(EVENT_TYPE_STRING + "=" + eventInfo.currEventType);
	}
	
}
