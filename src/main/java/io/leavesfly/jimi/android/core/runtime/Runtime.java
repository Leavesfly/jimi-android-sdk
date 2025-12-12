package io.leavesfly.jimi.android.core.runtime;

import io.leavesfly.jimi.android.llm.LLM;
import io.leavesfly.jimi.android.sdk.JimiConfig;

/**
 * Runtime 接口
 * 
 * 提供运行时配置和服务访问
 */
public interface Runtime {
    
    /**
     * 获取配置
     */
    JimiConfig getConfig();
    
    /**
     * 获取工作目录
     */
    String getWorkDir();
    
    /**
     * 获取 LLM 实例
     */
    LLM getLLM();
    
    /**
     * 设置 LLM 实例
     */
    void setLLM(LLM llm);
}
