package io.leavesfly.jimi.android.sdk;

/**
 * Jimi SDK 配置类
 */
public class JimiConfig {
    
    private String modelName;
    private String baseUrl;
    private String apiKey;
    private int maxContextSize = 8000;
    private int maxStepsPerRun = 10;
    private String workDir = "/tmp";
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public int getMaxContextSize() {
        return maxContextSize;
    }
    
    public void setMaxContextSize(int maxContextSize) {
        this.maxContextSize = maxContextSize;
    }
    
    public int getMaxStepsPerRun() {
        return maxStepsPerRun;
    }
    
    public void setMaxStepsPerRun(int maxStepsPerRun) {
        this.maxStepsPerRun = maxStepsPerRun;
    }
    
    public String getWorkDir() {
        return workDir;
    }
    
    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }
    
    /**
     * 验证配置
     */
    public void validate() {
        if (modelName == null || modelName.isEmpty()) {
            throw new IllegalArgumentException("modelName cannot be null or empty");
        }
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl cannot be null or empty");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("apiKey cannot be null or empty");
        }
        if (maxContextSize <= 0) {
            throw new IllegalArgumentException("maxContextSize must be positive");
        }
        if (maxStepsPerRun <= 0) {
            throw new IllegalArgumentException("maxStepsPerRun must be positive");
        }
    }
    
    /**
     * Builder 模式
     */
    public static class Builder {
        private JimiConfig config = new JimiConfig();
        
        public Builder modelName(String modelName) {
            config.modelName = modelName;
            return this;
        }
        
        public Builder apiEndpoint(String baseUrl) {
            config.baseUrl = baseUrl;
            return this;
        }
        
        public Builder apiKey(String apiKey) {
            config.apiKey = apiKey;
            return this;
        }
        
        public Builder maxContextSize(int maxContextSize) {
            config.maxContextSize = maxContextSize;
            return this;
        }
        
        public Builder maxStepsPerRun(int maxStepsPerRun) {
            config.maxStepsPerRun = maxStepsPerRun;
            return this;
        }
        
        public Builder workDir(String workDir) {
            config.workDir = workDir;
            return this;
        }
        
        public JimiConfig build() {
            return config;
        }
    }
}
