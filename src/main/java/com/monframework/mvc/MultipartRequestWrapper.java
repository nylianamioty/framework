package com.monframework.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.util.*;

public class MultipartRequestWrapper extends HttpServletRequestWrapper {
    
    private final Map<String, String[]> parameters = new HashMap<>();
    private final Map<String, List<UploadedFile>> files = new HashMap<>();
    
    public MultipartRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        
        try {
            // Extraire les paramètres normaux
            request.getParameterMap().forEach(parameters::put);
            
            // Extraire les fichiers
            Collection<Part> parts = request.getParts();
            for (Part part : parts) {
                if (part.getSubmittedFileName() != null && !part.getSubmittedFileName().isEmpty()) {
                    // C'est un fichier
                    UploadedFile uploadedFile = new UploadedFile(part);
                    String paramName = part.getName();
                    
                    files.computeIfAbsent(paramName, k -> new ArrayList<>())
                         .add(uploadedFile);
                    
                    // Ajouter le nom du fichier comme paramètre aussi
                    parameters.computeIfAbsent(paramName, k -> new String[0]);
                    String[] currentValues = parameters.get(paramName);
                    String[] newValues = Arrays.copyOf(currentValues, currentValues.length + 1);
                    newValues[currentValues.length] = uploadedFile.getOriginalFilename();
                    parameters.put(paramName, newValues);
                } else {
                    // C'est un paramètre normal
                    String paramName = part.getName();
                    String paramValue = request.getParameter(paramName);
                    if (paramValue != null) {
                        parameters.put(paramName, new String[]{paramValue});
                    }
                }
            }
            
        } catch (Exception e) {
            throw new IOException("Erreur lors du traitement multipart", e);
        }
    }
    
    @Override
    public String getParameter(String name) {
        String[] values = parameters.get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }
    
    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(parameters);
    }
    
    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }
    
    public List<UploadedFile> getUploadedFiles(String name) {
        return files.getOrDefault(name, Collections.emptyList());
    }
    
    public Map<String, List<UploadedFile>> getAllUploadedFiles() {
        return Collections.unmodifiableMap(files);
    }
    
    public UploadedFile getUploadedFile(String name) {
        List<UploadedFile> fileList = files.get(name);
        return fileList != null && !fileList.isEmpty() ? fileList.get(0) : null;
    }
}