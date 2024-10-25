package com.hyp.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadRequest {

	String fileUrl;
	String folderName;
	String fileName;

}
