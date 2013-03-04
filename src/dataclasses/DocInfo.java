package dataclasses;

import tempeval.EventTagger;

public class DocInfo {
	public String filename;
	public String id;
	public TimeInfo dctTimeInfo;
	public String dct;
	public String title;
	public String extra;
	
	public DocInfo(String filename, String id, String dct, String title, String extra) {
		this.filename = filename;
		this.id = id;
		this.dct = dct;
		this.dctTimeInfo = new TimeInfo(dct);
		this.title = title;
		this.extra = extra;
	}
}
