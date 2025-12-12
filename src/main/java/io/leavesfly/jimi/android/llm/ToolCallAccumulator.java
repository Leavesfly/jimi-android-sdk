package io.leavesfly.jimi.android.llm;

import io.leavesfly.jimi.android.llm.message.FunctionCall;
import io.leavesfly.jimi.android.llm.message.ToolCall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具调用累加器
 * 用于在流式响应中累积工具调用信息
 * <p>
 * 流式响应中，工具调用信息是分片返回的：
 * - 第一个 chunk 包含 id、type 和 function.name
 * - 后续 chunks 包含 function.arguments 的增量
 */
public class ToolCallAccumulator {

    /**
     * 按索引存储的工具调用构建器
     */
    private final Map<Integer, ToolCallBuilder> builders = new HashMap<>();

    /**
     * 累积工具调用增量
     *
     * @param delta 工具调用增量
     */
    public void accumulate(ToolCallDelta delta) {
        int index = delta.getIndex();
        ToolCallBuilder builder = builders.get(index);

        if (builder == null) {
            builder = new ToolCallBuilder();
            builders.put(index, builder);
        }

        // 累积 ID（仅第一次出现）
        if (delta.getId() != null) {
            builder.id = delta.getId();
        }

        // 累积类型（仅第一次出现）
        if (delta.getType() != null) {
            builder.type = delta.getType();
        }

        // 累积函数名称（仅第一次出现）
        if (delta.getFunctionName() != null) {
            builder.functionName = delta.getFunctionName();
        }

        // 累积函数参数（增量）
        if (delta.getFunctionArguments() != null) {
            builder.argumentsBuilder.append(delta.getFunctionArguments());
        }
    }

    /**
     * 构建最终的工具调用列表
     *
     * @return 工具调用列表，如果没有则返回 null
     */
    public List<ToolCall> build() {
        if (builders.isEmpty()) {
            return null;
        }

        List<ToolCall> result = new ArrayList<>();

        // 按索引顺序构建
        int maxIndex = 0;
        for (int index : builders.keySet()) {
            if (index > maxIndex) {
                maxIndex = index;
            }
        }

        for (int i = 0; i <= maxIndex; i++) {
            ToolCallBuilder builder = builders.get(i);
            if (builder != null) {
                result.add(builder.build());
            }
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * 是否有工具调用
     */
    public boolean hasToolCalls() {
        return !builders.isEmpty();
    }

    /**
     * 清空累积的数据
     */
    public void clear() {
        builders.clear();
    }

    /**
     * 内部构建器类
     */
    private static class ToolCallBuilder {
        String id;
        String type = "function";
        String functionName;
        StringBuilder argumentsBuilder = new StringBuilder();

        ToolCall build() {
            FunctionCall function = new FunctionCall(functionName, argumentsBuilder.toString());
            return new ToolCall(id, function);
        }
    }
}
