package com.monframework.mvc;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {
    private String view;
    private Map<String, Object> model = new HashMap<>();

    public ModelAndView() {}
    
    public ModelAndView(String view) {
        this.view = view;
    }

    public String getView() { 
        return view; 
    }
    
    public void setView(String view) { 
        this.view = view; 
    }
    
    public Map<String, Object> getModel() { 
        return model; 
    }
    
    public void addObject(String key, Object value) {
        this.model.put(key, value);
    }
    
    public void setModel(Map<String, Object> model) {
        this.model = model;
    }
    
    // Pour compatibilité avec ModelView existant
    public Map<String, Object> getData() {
        return model;
    }
}