package tempeval;

import java.io.*;
import java.util.*;

import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.*;

public class EventTagger {

	/*
	 * Adds event annotations to annotation object by stripping out EVENT
	 * tags and adding their information to the tokens they contain. Also
	 * strips out TIMEX3 and SIGNAL tags.
	 */
	

	
	
	private static class EventInfo{
		private String currEventType;
		private String currEventId;
		
		private EventInfo(String currEventType, String currEventID){
			this.currEventType = currEventType;
			this.currEventId = currEventID;
		}
		
		private String getEventType(){
			return this.currEventType;
		}
		
		private String getEventId(){
			return this.currEventId;
		}
	}
	
	private static class TimeInfo{
		//could have added others, but on TempEval site it was stated that Type and
		private String currTimeType;
		private String currTimeId;
		private String currTimeValue;
		private String currTimeTemporalFunction;
		private String currTimefunctionInDocument;
		
		private TimeInfo(String currTimeType, String currTimeId, String currTimeValue, String currTimeTemporalFunction, String currTimefunctionInDocument){
			this.currTimeType = currTimeType;
			this.currTimeId = currTimeId;
			this.currTimeValue = currTimeValue;
			this.currTimeTemporalFunction = currTimeTemporalFunction;
			this.currTimefunctionInDocument = currTimefunctionInDocument;
		}
		
		private String getTimeType(){
			return this.currTimeType;
		}
		
		private String getTimeId(){
			return this.currTimeId;
		}
		
		private String getTimeVal(){
			return this.currTimeValue;
		}
		
		private String getTimeTemporalFunction(){
			return this.currTimeTemporalFunction;
		}
		
		private String getFunctionInDocument(){
			return this.currTimefunctionInDocument;
		}

	}
	
	
	private static TimeInfo getCurrentTimeInfo(String word, CoreLabel token){
		
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
		
		start = extract.indexOf(TimeFunctionInDocumentString) +TimeFunctionInDocumentString.length();
		end = extract.indexOf("\"", start);
		String currTimeFunctionInDocument = extract.substring(start, end);
	
		
		return new TimeInfo(currTimeId, currTimeType, currTimeValue, currTimeTemporalF, currTimeFunctionInDocument);
	}
	private static EventInfo getCurrentEventInfo(String word, CoreLabel token){		
		String extract = new String(word);
		
		String EventIdString = "eid=\"";
		String EventTypeString = "class=\"";
		
		int start = extract.indexOf(EventIdString) + EventIdString.length();
		int end = extract.indexOf("\"", start);
		String currEventId = extract.substring(start, end);
		
		start = extract.indexOf(EventTypeString) + EventTypeString.length();
		end = extract.indexOf("\"", start);
		String currEventType = extract.substring(start, end);
		
		
		return new EventInfo(currEventType, currEventId);
	}
	
	public static void annotate(Annotation annotation) {
		
		// Keep track of current event type
		EventInfo currEvent = null;
		TimeInfo currTime = null;
		String currTag = "O";

		// Add event information to each event-tagged token
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			
			// Keep track of tokens to remove
			Set<CoreLabel> tokensToRemove = new HashSet<CoreLabel>();
			
			// Annotate each token
			for (CoreLabel token: tokens) {				
				String word = token.get(TextAnnotation.class);
				
				// Assign currTag type
					
				if(word.startsWith("<EVENT")){
						currTag = "EVENT";
						currEvent = getCurrentEventInfo(word, token);
						tokensToRemove.add(token);
				} else if(word.startsWith("<TIMEX3")){
						currTime = getCurrentTimeInfo(word, token);
						currTag = "TIME";
						tokensToRemove.add(token);
				}  else if (word.startsWith("</")) {
					currTag = "O";
					tokensToRemove.add(token);
				} else if (currTag == "EVENT"){
					token.set(EventClassAnnotation.class, currEvent.getEventType());
					token.set(EventIdAnnotation.class, currEvent.getEventId());
				} else if (currTag == "TIME"){
					//TODO assign appropriate annotation to token, create classes for annotations
					tokensToRemove.add(token);
				}
			}
			
			// Remove tokens corresponding to tags
			for (CoreLabel token: tokensToRemove) {
				tokens.remove(token);
			}
		}
	}
	
	/*
	 * Prints event annotations in two-column format.
	 */
	public static void printEventAnnotations(Annotation annotation, BufferedWriter out) throws IOException {
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				String word = token.getString(TextAnnotation.class);
				String event = token.getString(EventClassAnnotation.class);
				
				out.write(word + " ");
				if(event != "")
					out.write(event);
				else
					out.write("O");
				out.write("\n");
				out.flush();
			}
		}
	}

}
