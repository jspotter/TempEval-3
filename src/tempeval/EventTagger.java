package tempeval;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;
import org.w3c.dom.Document;

import annotationclasses.AuxTokenInfoAnnotation;
import annotationclasses.EventAnnotation;
import annotationclasses.SignalAnnotation;
import annotationclasses.TimeAnnotation;

import dataclasses.AuxTokenInfo;
import dataclasses.EventInfo;
import dataclasses.TimeInfo;

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

	private static final String CRF_CLASSIFIER_FILENAME = 
			"classifiers/event-model.ser.gz";

	private static AbstractSequenceClassifier classifier = null;

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
	private static EventInfo getCurrentEventInfo(String word, CoreLabel token, 
			Document doc) {
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
	 * Add event information to the provided annotation. Also add other
	 * information, like token number and next/previous tokens.
	 */
	public static void annotate(Annotation annotation, Document doc) {

		// Keep track of current event type
		EventInfo currEvent = null;
		TimeInfo currTime = null;
		String currTag = "O";

		// Add event information to each event-tagged token
		List<CoreMap> sentences = 
				annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {

			CoreLabel lastToken = null;
			AuxTokenInfo lastTokenAux = null;
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);

			// Keep track of tokens to remove
			Set<CoreLabel> tokensToRemove = new HashSet<CoreLabel>();

			// Annotate each token
			int curTokenNum = 0;
			for (CoreLabel token: tokens) {
				String word = token.get(TextAnnotation.class);

				// Assign currTag type if we're looking at a tag
				if(word.startsWith("<EVENT")) {
					currTag = "EVENT";
					currEvent = getCurrentEventInfo(word, token, doc);
					tokensToRemove.add(token);
				} else if(word.startsWith("<TIMEX3")) {
					currTime = getCurrentTimeInfo(word, token);
					currTag = "TIME";
					tokensToRemove.add(token);
				} else if (word.startsWith("<SIGNAL")) {
					currTag = "SIGNAL";
					tokensToRemove.add(token);
				}  else if (word.startsWith("</")) {
					currTag = "O";
					tokensToRemove.add(token);

					// Otherwise, we're looking at a token
				} else {

					// Handle general token annotations
					AuxTokenInfo aux = new AuxTokenInfo();
					aux.tokenOffset = curTokenNum++;
					aux.prev = lastToken;
					aux.next = null;
					if (lastTokenAux != null)
						lastTokenAux.next = token;

					token.set(AuxTokenInfoAnnotation.class, aux);

					lastToken = token;
					lastTokenAux = aux;

					// Handle event-specific token annotations
					if (currTag == "EVENT") {
						token.set(EventAnnotation.class, currEvent);
						currEvent.numTokens++;

						// Handle time-specific token annotations
					} else if (currTag == "TIME") {
						token.set(TimeAnnotation.class, currTime);
						currTime.numTokens++;

						// Handle signal-specific token annotations
					} else if (currTag == "SIGNAL") {
						token.set(SignalAnnotation.class, true);
					}
				}
			}

			// Remove tokens corresponding to tags
			for (CoreLabel token: tokensToRemove) {
				tokens.remove(token);
			}
		}
	}

	/*
	 * Helper method for testEventTagger
	 */
	public static String getWordEventType(int index, String [] tagged_data_array){
		String tag = tagged_data_array[index];
		int start = tag.lastIndexOf("/");
		return tag.substring(start + 1);
	}

	public static void loadTestClassifier() {
		classifier = 
				CRFClassifier.getClassifierNoExceptions(CRF_CLASSIFIER_FILENAME);
	}

	/*
	 * Method to run at test that given our annotations will annotate tokens classified as Events with the appropriate EventInfo object
	 * Must call loadTestClassifier first!
	 */
	public static void testEventTagger(Annotation annotation) {

		String data = "";
		List<CoreMap> sentences = 
				annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			for (CoreLabel token: tokens) {
				String word = token.get(TextAnnotation.class);
				data += word + " ";
			}
		}


		String [] tagged_data_array = classifier.classifyToString(data).split("\\s+");

		//Perform a mapping from tagged_data_array event classifications to annotations, 
		//all classifier output appears to tokenize identically to the coreNLP annotator
		int count = 0;
		sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			for (CoreLabel token: tokens) {
				String word = token.get(TextAnnotation.class);
				String event_type = getWordEventType(count, tagged_data_array);
				if(!event_type.equals("O")) {
					
					// No event ID needed - this will be handled by
					// the AnnotationWriter.
					token.set(EventAnnotation.class, new EventInfo(event_type, "0"));
				}
				count++;
			}
		}

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
