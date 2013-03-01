package helperclasses;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import annotationclasses.DocInfoAnnotation;
import annotationclasses.EventAnnotation;
import annotationclasses.LinkInfoAnnotation;

import dataclasses.DocInfo;
import dataclasses.EventInfo;
import dataclasses.LinkInfo;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.util.CoreMap;

public class AnnotationWriter {

	public static final String HEADER = "<?xml version=\"1.0\" ?>\n"
			+ "<TimeML xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
			+ "xsi:noNamespaceSchemaLocation=\"http://timeml.org/timeMLdocs/TimeML_1.2.1.xsd\">\n\n";
	public static final String FOOTER = "</TimeML>\n\n";
	
	/*
	 * Taken from http://www.java2s.com/Code/Java/Data-Type/Returnstrueifspecifiedcharacterisapunctuationcharacter.htm
	 */
	private static boolean isPunctuation(char c) {
		return c == ','
	            || c == '.'
	            || c == '!'
	            || c == '?'
	            || c == ':'
	            || c == ';'
	            || c == '"'
	            || c == '\''
	            || c == '`';
	}
	
	private static void writeText(Annotation annotation, BufferedWriter out,
			ArrayList<EventInfo> events, ArrayList<LinkInfo> links) throws IOException {
		
		int nextEventID = 0;
		
		// Keep track of whether we're inside a tag
		String curType = "";
		String curEndTag = "";
		
		out.write("<TEXT>\n\n");
		
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			for (CoreLabel token: tokens) {
				
				// Get information from token
				Timex timex = token.get(TimexAnnotation.class);
				EventInfo event = token.get(EventAnnotation.class);
				LinkInfo link = token.get(LinkInfoAnnotation.class);
				String text = token.get(TextAnnotation.class);
				
				//TODO fix this, and in general have better whitespace recovery
				String space = (isPunctuation(text.charAt(0)) ? "" : " ");
				
				// Handle if this is a timex
				if (timex != null && !curType.equals(timex.timexType())) {
					out.write(" <TIMEX3 tid=\"" + timex.tid() + "\" type=\""
							+ timex.timexType() + "\" value=\"" + timex.value()
							+ "\">" + text);
					curType = timex.timexType();
					curEndTag = "</TIMEX3>";
					
				// Handle if this is an event
				} else if (event != null && !curType.equals(event.currEventType)) {
					out.write(" <EVENT eid=\"e" + (nextEventID++) + "\" class=\""
							+ event.currEventType + "\">" + text);
					curType = event.currEventType;
					curEndTag = "</EVENT>";
					events.add(event);
					
				// Handle normal tokens
				} else {
					out.write(curEndTag + space + text);
					curType = "";
					curEndTag = "";
				}
				
				// Handle links
				if (link != null) {
					links.add(link);
				}
			}
		}
		
		out.write("\n\n</TEXT>\n\n");
	}
	
	private static void writeEventInstances(ArrayList<EventInfo> events, BufferedWriter out)
			throws IOException {
		for (EventInfo event: events) {
			out.write("<MAKEINSTANCE eventID=\"" + event.currEventId 
					+ "\" eiid=\"" + event.currEiid + "\" tense=\"" + event.tense
					+ "\" aspect=\"" + event.aspect + "\" polarity=\"" + event.polarity
					+ "\" pos=\"" + event.pos + "\"/>\n");
		}
	}
	
	private static void writeTimexEventLinks(ArrayList<LinkInfo> links, BufferedWriter out)
			throws IOException {
		for (LinkInfo link: links) {
			String instanceString = "";
			if (link.time != null)
				instanceString = "timeID=\"" + link.time.currTimeId + "\"";
			else
				instanceString = "eventInstanceID=\"" + link.eventInstance.currEiid + "\"";
			
			out.write("<TLINK lid=\"" + link.id + "\" relType=\"" + link.type
					+ "\" " + instanceString + " relatedToEventInstance=\"" + link.relatedTo.currEiid
					+ "\" origin=\"USER\"/>\n"); //TODO fix origin
		}
	}
	
	public static void writeAnnotation(Annotation annotation, BufferedWriter out)
			throws IOException {
		
		ArrayList<EventInfo> events = new ArrayList<EventInfo>();
		ArrayList<LinkInfo> links = new ArrayList<LinkInfo>();
		
		// Get the auxiliary information associated with this document
		DocInfo info = annotation.get(DocInfoAnnotation.class);
		
		out.write(HEADER);
		out.write("<DOCID>" + info.id + "</DOCID>\n\n");
		out.write("<DCT>" + info.dct + "</DCT>\n\n");
		out.write("<TITLE>" + info.title + "</TITLE>\n\n");
		out.write("<EXTRAINFO>" + info.extra + "</EXTRAINFO>\n\n");
		
		writeText(annotation, out, events, links);
		writeEventInstances(events, out);
		writeTimexEventLinks(links, out);
		
		out.write(FOOTER);
	}
}
