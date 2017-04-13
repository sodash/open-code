package com.winterwell.maths.chart;

public enum ImageFormat {
	PNG("png"),
	JPEG("jpeg"),
	GIF("gif"),
	PDF("pdf");
	
	private final String fileExtension;
	
	private ImageFormat(String fileExtension) {
		this.fileExtension = fileExtension;
	}
	
	public String getFileExtension() {
		return this.fileExtension;
	}
}
