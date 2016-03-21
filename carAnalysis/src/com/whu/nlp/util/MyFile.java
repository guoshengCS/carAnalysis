package com.whu.nlp.util;

public class MyFile {
	
	String fileId;
	String content;
	
	public MyFile(String fileId, String content) {
		super();
		this.fileId = fileId;
		this.content = content;
	}
	public String getFileId() {
		return fileId;
	}
	public void setFileId(String fileId) {
		this.fileId = fileId;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
}
