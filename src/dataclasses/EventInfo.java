package dataclasses;

import helperclasses.XMLParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/*
 * Adds event annotations to annotation object by stripping out EVENT
 * tags and adding their information to the tokens they contain. Also
 * strips out TIMEX3 and SIGNAL tags.
 */

public class EventInfo{
	public String currEventType;
	public String currEventId;
	public String currEiid;
	public String tense;
	public String aspect;
	public String polarity;
	public String pos;
	public int numTokens;

public EventInfo(String currEventType, String currEventID) {
		this.currEventType = currEventType;
		this.currEventId = currEventID;
		this.currEiid = null;
		this.tense = null;
		this.aspect = null;
		this.polarity = null;
		this.pos = null;
		this.numTokens = 0;
	}

	
public EventInfo(String word, Document doc) {
		String extract = new String(word);

		String EventIdString = "eid=\"";
		String EventTypeString = "class=\"";

		int start = extract.indexOf(EventIdString) + EventIdString.length();
		int end = extract.indexOf("\"", start);
		String currEventId = extract.substring(start, end);

		start = extract.indexOf(EventTypeString) + EventTypeString.length();
		end = extract.indexOf("\"", start);
		String currEventType = extract.substring(start, end);

		this.currEventType = currEventType;
		this.currEventId = currEventId;
		this.currEiid = null;
		this.tense = null;
		this.aspect = null;
		this.polarity = null;
		this.pos = null;
		this.numTokens = 0;
		getAuxEventInfo(doc);
	}

	public void getAuxEventInfo(Document doc) {
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