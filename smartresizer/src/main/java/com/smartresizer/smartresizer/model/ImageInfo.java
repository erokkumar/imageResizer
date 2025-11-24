package com.smartresizer.smartresizer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageInfo {
    private String fileName;
    private String resizedFilePath;
    private int originalWidth;
    private int originalHeight;
    private long originalSizeKB;
    private int newWidth;
    private int newHeight;
    private long newSizeKB;
    private String resizedFileName;
}
