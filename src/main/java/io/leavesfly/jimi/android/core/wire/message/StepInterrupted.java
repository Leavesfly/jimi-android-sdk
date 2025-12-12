package io.leavesfly.jimi.android.core.wire.message;

/**
 * 步骤中断消息
 */
public class StepInterrupted extends WireMessage {
    
    @Override
    public String getType() {
        return "step_interrupted";
    }
}
