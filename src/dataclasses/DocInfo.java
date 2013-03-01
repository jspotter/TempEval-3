package dataclasses;

public class DocInfo {
	public String filename;
	public String id;
	public String dct;
	public String title;
	public String extra;
	
	public DocInfo(String filename, String id, String dct, String title, String extra) {
		this.filename = filename;
		this.id = id;
		this.dct = dct;
		this.title = title;
		this.extra = extra;
	}
}
