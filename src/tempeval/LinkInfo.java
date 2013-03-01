package tempeval;

public class LinkInfo {
	public String id;
	public String type;
	public String timeID;			// timeID and eventInstance are mutually exclusive.
	public String eventInstance;	// The first part of the link is one or the other.
	public String relatedTo;
	
	public LinkInfo(String id, String type, String timeID, String eventInstance,
			String relatedTo) {
		this.id = id;
		this.type = type;
		this.timeID = timeID;
		this.eventInstance = eventInstance;
		this.relatedTo = relatedTo;
	}
}
