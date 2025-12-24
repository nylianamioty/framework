package com.monframework.mvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.http.Part;

public class UploadedFile {
    private String name;
    private String contentType;
    private byte[] content;
    private long size;
    
    // Constructeurs
    public UploadedFile() {}
    
    public UploadedFile(String name, String contentType, byte[] content, long size) {
        this.name = name;
        this.contentType = contentType;
        this.content = content;
        this.size = size;
    }
    public UploadedFile(Part part) throws IOException {
    if (part != null) {
        this.name = extractFileName(part);
        this.contentType = part.getContentType();
        this.size = part.getSize();
        
        // Lire le contenu
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream inputStream = part.getInputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
        }
        this.content = baos.toByteArray();
    }
}
private String extractFileName(Part part) {
    String contentDisp = part.getHeader("content-disposition");
    String[] items = contentDisp.split(";");
    for (String s : items) {
        if (s.trim().startsWith("filename")) {
            return s.substring(s.indexOf('=') + 2, s.length() - 1);
        }
    }
    return "";
}

// Ajoute cette méthode pour compatibilité
public String getOriginalFilename() {
    return this.name;
}
    // Getters et setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }
    
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    
    // Méthodes utilitaires
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }
    
    public String getExtension() {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf(".") + 1);
    }
}