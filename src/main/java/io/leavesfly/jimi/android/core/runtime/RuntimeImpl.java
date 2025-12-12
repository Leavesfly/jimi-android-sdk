package io.leavesfly.jimi.android.core.runtime;

import io.leavesfly.jimi.android.llm.LLM;
import io.leavesfly.jimi.android.sdk.JimiConfig;

/**
 * Runtime 实现类
 */
public class RuntimeImpl implements Runtime {
    
    private final JimiConfig config;
    private LLM llm;
    
    public RuntimeImpl(JimiConfig config) {
        this.config = config;
    }
    
    public RuntimeImpl(JimiConfig config, LLM llm) {
        this.config = config;
        this.llm = llm;
    }
    
    @Override
    public JimiConfig getConfig() {
        return config;
    }
    
    @Override
    public String getWorkDir() {
        return config.getWorkDir();
    }
    
    @Override
    public LLM getLLM() {
        return llm;
    }
    
    @Override
    public void setLLM(LLM llm) {
        this.llm = llm;
    }
}
