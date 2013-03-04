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
	
	public TimeInfo (String word) {

		String extract = new String(word);

		String TimeIdString = "tid=\"";
		String TimeTypeString = "type=\"";
		String TimeValueString = "value=\"";
		String TimeTemporalFString = "temporalFunction=\"";
		String TimeFunctionInDocumentString = "functionInDocument=\"";

		int start = extract.indexOf(TimeIdString) + TimeIdString.length();
		int end = extract.indexOf("\"", start);
		String currTimeId = extract.substring(start, end);

		start = extract.indexOf(TimeTypeString) + TimeTypeString.length();
		end = extract.indexOf("\"", start);
		String currTimeType = extract.substring(start, end);

		start = extract.indexOf(TimeValueString) +TimeValueString.length();
		end = extract.indexOf("\"", start);
		String currTimeValue = extract.substring(start, end);

		start = extract.indexOf(TimeTemporalFString) + TimeTemporalFString.length();
		end = extract.indexOf("\"", start);
		String currTimeTemporalF = extract.substring(start, end);

		start = extract.indexOf(TimeFunctionInDocumentString) 
				+ TimeFunctionInDocumentString.length();
		end = extract.indexOf("\"", start);
		String currTimeFunctionInDocument = extract.substring(start, end);
		
		this.currTimeType = currTimeType;
		this.currTimeId = currTimeId;
		this.currTimeValue = currTimeValue;
		this.currTimeTemporalFunction = currTimeTemporalF;
		this.currTimeFunctionInDocument = currTimeFunctionInDocument;
		this.numTokens = 0;
	}
}
