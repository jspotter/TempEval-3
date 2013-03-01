package dataclasses;

public class TimeInfo{
	//could have added others, but on TempEval site it was stated that Type and
	public String currTimeType;
	public String currTimeId;
	public String currTimeValue;
	public String currTimeTemporalFunction;
	public String currTimeFunctionInDocument;
	public int numTokens;

	public TimeInfo(String currTimeId, String currTimeType, String currTimeValue, 
			String currTimeTemporalFunction, String currTimeFunctionInDocument) {
		this.currTimeType = currTimeType;
		this.currTimeId = currTimeId;
		this.currTimeValue = currTimeValue;
		this.currTimeTemporalFunction = currTimeTemporalFunction;
		this.currTimeFunctionInDocument = currTimeFunctionInDocument;
		this.numTokens = 0;
	}
}
