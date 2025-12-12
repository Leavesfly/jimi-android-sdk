package io.leavesfly.jimi.android.core.wire.message;

/**
 * 步骤开始消息
 */
public class StepBegin extends WireMessage {
    
    private final int stepNo;
    private final boolean isSubagent;
    private final String agentName;
    
    public StepBegin(int stepNo, boolean isSubagent, String agentName) {
        this.stepNo = stepNo;
        this.isSubagent = isSubagent;
        this.agentName = agentName;
    }
    
    public StepBegin(int stepNo) {
        this(stepNo, false, null);
    }
    
    @Override
    public String getType() {
        return "step_begin";
    }
    
    public int getStepNo() {
        return stepNo;
    }
    
    public boolean isSubagent() {
        return isSubagent;
    }
    
    public String getAgentName() {
        return agentName;
    }
}
