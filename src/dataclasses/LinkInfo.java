package dataclasses;

import java.util.ArrayList;
import java.util.Set;

import edu.stanford.nlp.util.CollectionUtils;

public class LinkInfo {
	private ArrayList<Link> links;
	
	public LinkInfo(){
		links = new ArrayList<Link>();
	}
	
	public void addLink(String id, String type, TimeInfo time, EventInfo eventInstance,
				EventInfo relatedTo){
		links.add(new Link(id, type, time, eventInstance, relatedTo));
	}
	
	public ArrayList<Link> getLinks(){
		return links;
	}
	
	public class Link {

		public String id;
		public String type;
		public TimeInfo time;			// timeID and eventInstance are mutually exclusive.
		public EventInfo eventInstance;	// The first part of the link is one or the other.
		public EventInfo relatedTo;

		private final String[] validLinkTypes = new String[]{
			"BEFORE", "AFTER", "INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "SIMULTANEOUS", 
			"IAFTER", "IBEFORE", "IDENTITY", "BEGINS", "ENDS", "BEGUN_BY", "ENDED_BY", "OVERLAP",
			"BEFORE-OR-OVERLAP", "OVERLAP-OR-AFTER", "VAGUE", "NONE"
		};
		
		private final Set<String> LINK_TYPES = CollectionUtils.asSet(validLinkTypes);

		
		public Link(String id, String type, TimeInfo time, EventInfo eventInstance,
				EventInfo relatedTo) {
			this.id = id;
			this.type = type;
			this.time = time;
			this.eventInstance = eventInstance;
			this.relatedTo = relatedTo;
			if (!LINK_TYPES.contains(this.type)) {
				System.err.println("Unknown link type: " + this.type);
				this.type = "NONE";
			}
		}
	}
	
	
}
