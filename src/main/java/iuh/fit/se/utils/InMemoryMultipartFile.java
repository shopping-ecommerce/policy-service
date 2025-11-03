package iuh.fit.se.utils;


import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class InMemoryMultipartFile implements MultipartFile {
    private final String name, originalFilename, contentType;
    private final byte[] content;

    public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
        this.name = name; this.originalFilename = originalFilename; this.contentType = contentType; this.content = content;
    }
    public String getName() { return name; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public boolean isEmpty() { return content.length == 0; }
    public long getSize() { return content.length; }
    public byte[] getBytes() { return content; }
    public InputStream getInputStream() { return new ByteArrayInputStream(content); }
    public void transferTo(File dest) throws IOException { try (FileOutputStream fos = new FileOutputStream(dest)) { fos.write(content); } }
}