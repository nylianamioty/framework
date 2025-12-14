package com.monframework.mvc;

import java.util.List;
import java.util.Map;

public class JsonResponse {
    private String status;
    private int code;
    private Object data;
    private Integer count;
    
    public JsonResponse() {}
    
    public JsonResponse(String status, int code, Object data) {
        this.status = status;
        this.code = code;
        this.data = data;
        
        // Calculer count si data est une liste ou tableau
        if (data instanceof List) {
            this.count = ((List<?>) data).size();
        } else if (data != null && data.getClass().isArray()) {
            this.count = java.lang.reflect.Array.getLength(data);
        } else if (data instanceof Map) {
            this.count = ((Map<?, ?>) data).size();
        }
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    
    public Object getData() { return data; }
    public void setData(Object data) { 
        this.data = data;
        
        // Recalculer count
        if (data instanceof List) {
            this.count = ((List<?>) data).size();
        } else if (data != null && data.getClass().isArray()) {
            this.count = java.lang.reflect.Array.getLength(data);
        } else if (data instanceof Map) {
            this.count = ((Map<?, ?>) data).size();
        }
    }
    
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}