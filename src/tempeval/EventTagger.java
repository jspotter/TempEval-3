package tempeval;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;
import org.w3c.dom.Document;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;

import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.util.StringUtils;

public class EventTagger {

	/*
	 * Adds event annotations to annotation object by stripping out EVENT
	 * tags and adding their information to the tokens they contain. Also
	 * strips out TIMEX3 and SIGNAL tags.
	 */

	public static class EventInfo{
		public String currEventType;
		public String currEventId;
		public String currEiid;
		public String tense;
		public String aspect;
		public String polarity;
		public String pos;
		public int numTokens;

		private EventInfo(String currEventType, String currEventID) {
			this.currEventType = currEventType;
			this.currEventId = currEventID;
			this.currEiid = null;
			this.tense = null;
			this.aspect = null;
			this.polarity = null;
			this.pos = null;
			this.numTokens = 0;
		}

		private void getAuxEventInfo(Document doc) {
			Element root = doc.getDocumentElement();

			Element[] auxEventInfoElems = 
					XMLParser.getElementsByTagNameNR(root, "MAKEINSTANCE");
			for(Element e : auxEventInfoElems) {
				String id = e.getAttribute("eventID");
				if(id != null && id.equals(currEventId)) {
					currEiid = e.getAttribute("eiid");
					tense = e.getAttribute("tense");
					aspect = e.getAttribute("aspect");
					polarity = e.getAttribute("polarity");
					pos = e.getAttribute("pos");
					return;
				}
			}
		}
	}

	public static class TimeInfo{
		//could have added others, but on TempEval site it was stated that Type and
		public String currTimeType;
		public String currTimeId;
		public String currTimeValue;
		public String currTimeTemporalFunction;
		public String currTimeFunctionInDocument;
		public int numTokens;

		private TimeInfo(String currTimeId, String currTimeType, String currTimeValue, 
				String currTimeTemporalFunction, String currTimeFunctionInDocument) {
			this.currTimeType = currTimeType;
			this.currTimeId = currTimeId;
			this.currTimeValue = currTimeValue;
			this.currTimeTemporalFunction = currTimeTemporalFunction;
			this.currTimeFunctionInDocument = currTimeFunctionInDocument;
			this.numTokens = 0;
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

		start = extract.indexOf(TimeFunctionInDocumentString) 
				+ TimeFunctionInDocumentString.length();
		end = extract.indexOf("\"", start);
		String currTimeFunctionInDocument = extract.substring(start, end);


		return new TimeInfo(currTimeId, currTimeType, currTimeValue, 
				currTimeTemporalF, currTimeFunctionInDocument);
	}

	/*
	 * Extracts information about a single event from training data.
	 */
	private static EventInfo getCurrentEventInfo(String word, CoreLabel token, Document doc) {
		String extract = new String(word);

		String EventIdString = "eid=\"";
		String EventTypeString = "class=\"";

		int start = extract.indexOf(EventIdString) + EventIdString.length();
		int end = extract.indexOf("\"", start);
		String currEventId = extract.substring(start, end);

		start = extract.indexOf(EventTypeString) + EventTypeString.length();
		end = extract.indexOf("\"", start);
		String currEventType = extract.substring(start, end);

		EventInfo result = new EventInfo(currEventType, currEventId);
		result.getAuxEventInfo(doc);
		return result;
	}

	/*
	 * Add event information to the provided annotation.
	 */
	public static void annotate(Annotation annotation, Document doc) {

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
			int curTokenNum = 0;
			for (CoreLabel token: tokens) {
				String word = token.get(TextAnnotation.class);

				// Assign currTag type
				if(word.startsWith("<EVENT")){
					currTag = "EVENT";
					currEvent = getCurrentEventInfo(word, token, doc);
					tokensToRemove.add(token);
				} else if(word.startsWith("<TIMEX3")){
					currTime = getCurrentTimeInfo(word, token);
					currTag = "TIME";
					tokensToRemove.add(token);
				}  else if (word.startsWith("</")) {
					currTag = "O";
					tokensToRemove.add(token);
				} else if (currTag == "EVENT") {
					token.set(EventAnnotation.class, currEvent);
					token.set(TokenOffsetAnnotation.class, curTokenNum++);
					currEvent.numTokens++;
				} else if (currTag == "TIME") {
					token.set(TimeAnnotation.class, currTime);
					token.set(TokenOffsetAnnotation.class, curTokenNum++);
					currTime.numTokens++;
				}
			}

			// Remove tokens corresponding to tags
			for (CoreLabel token: tokensToRemove) {
				tokens.remove(token);
			}
		}
	}

	public static void testEventTagger(ArrayList<Annotation> annotations, String filepath){

		String data = "";

		for(Annotation annotation : annotations){
			List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
			for(CoreMap sentence: sentences) {

				List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);

				Set<CoreLabel> tokensToRemove = new HashSet<CoreLabel>();

				for (CoreLabel token: tokens) {
					String word = token.get(TextAnnotation.class);
					data += word + " ";
				}
			}
		}

		AbstractSequenceClassifier classifier = CRFClassifier.getClassifierNoExceptions(filepath);

		String tagged_data = classifier.classifyToString(data);


		//System.out.println(tagged_data);



	}

	/*
	 * Prints event annotations in two-column format.
	 */
	public static void printEventAnnotations(Annotation annotation, BufferedWriter out) 
			throws IOException {
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				String word = token.getString(TextAnnotation.class);
				EventInfo eventInfo = token.get(EventAnnotation.class);
				String event = "";
				if (eventInfo != null) {
					event = eventInfo.currEventType;
				}

				out.write(word + " ");
				if(!event.equals(""))
					out.write(event);
				else
					out.write("O");
				out.write("\n");
				out.flush();
			}
		}
	}

}
