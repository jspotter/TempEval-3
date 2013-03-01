package dataclasses;

public class LinkInfo {
	public String id;
	public String type;
	public TimeInfo time;			// timeID and eventInstance are mutually exclusive.
	public EventInfo eventInstance;	// The first part of the link is one or the other.
	public EventInfo relatedTo;
	
	public LinkInfo(String id, String type, TimeInfo time, EventInfo eventInstance,
			EventInfo relatedTo) {
		this.id = id;
		this.type = type;
		this.time = time;
		this.eventInstance = eventInstance;
		this.relatedTo = relatedTo;
	}
}
