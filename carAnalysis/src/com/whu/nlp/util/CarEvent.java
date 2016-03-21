package com.whu.nlp.util;

class CarEvent{
	String id;
	String fileId;//fileName
	String firstCategory;
	String eventName;
	String content;
	int polarity;
	public CarEvent(String id, String fileId, String firstCategory,String eventName, String content, int polarity) {
		super();
		this.id = id;
		this.fileId = fileId;
		this.firstCategory = firstCategory;
		this.eventName = eventName;
		this.content = content;
		this.polarity = polarity;
	}
	@Override
	public String toString() {
		return "CarEvent [id=" + id + ", fileId=" + fileId+", firstCategory=" + firstCategory+", eventName=" + eventName + ", content="
				+ content + ", polarity=" + polarity + "]";
	}
	public String getFirstCategory() {
		return firstCategory;
	}
	public void setFirstCategory(String firstCategory) {
		this.firstCategory = firstCategory;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getFileId() {
		return fileId;
	}
	public void setFileId(String fileId) {
		this.fileId = fileId;
	}
	public String getEventName() {
		return eventName;
	}
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public int getPolarity() {
		return polarity;
	}
	public void setPolarity(int polarity) {
		this.polarity = polarity;
	}
	
}
