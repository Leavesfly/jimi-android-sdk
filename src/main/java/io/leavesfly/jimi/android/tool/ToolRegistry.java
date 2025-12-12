package io.leavesfly.jimi.android.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册表
 * 管理所有可用工具的注册、查找和执行
 */
public class ToolRegistry {

    private final Map<String, Tool> tools = new HashMap<>();

    /**
     * 注册工具
     *
     * @param tool 工具实例
     */
    public void register(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }
        tools.put(tool.getName(), tool);
    }

    /**
     * 批量注册工具
     *
     * @param toolList 工具列表
     */
    public void registerAll(List<Tool> toolList) {
        for (Tool tool : toolList) {
            register(tool);
        }
    }

    /**
     * 注销工具
     *
     * @param name 工具名称
     */
    public void unregister(String name) {
        tools.remove(name);
    }

    /**
     * 获取工具
     *
     * @param name 工具名称
     * @return 工具实例，不存在返回 null
     */
    public Tool getTool(String name) {
        return tools.get(name);
    }

    /**
     * 检查工具是否存在
     *
     * @param name 工具名称
     * @return 是否存在
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    /**
     * 获取所有工具名称
     */
    public List<String> getToolNames() {
        return new ArrayList<>(tools.keySet());
    }

    /**
     * 获取指定工具的 Schema 列表
     *
     * @param toolNames 工具名称列表，null 表示获取所有
     * @return Schema 列表
     */
    public List<ToolSchema> getToolSchemas(List<String> toolNames) {
        List<ToolSchema> schemas = new ArrayList<>();

        if (toolNames == null || toolNames.isEmpty()) {
            // 返回所有工具的 Schema
            for (Tool tool : tools.values()) {
                schemas.add(tool.getSchema());
            }
        } else {
            // 返回指定工具的 Schema
            for (String name : toolNames) {
                Tool tool = tools.get(name);
                if (tool != null) {
                    schemas.add(tool.getSchema());
                }
            }
        }

        return schemas;
    }

    /**
     * 执行工具
     *
     * @param name      工具名称
     * @param arguments JSON 格式参数
     * @return 执行结果
     */
    public ToolResult execute(String name, String arguments) {
        Tool tool = tools.get(name);
        if (tool == null) {
            return ToolResult.error("Tool not found: " + name);
        }

        try {
            return tool.execute(arguments);
        } catch (Exception e) {
            return ToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * 获取已注册工具数量
     */
    public int size() {
        return tools.size();
    }

    /**
     * 清空所有工具
     */
    public void clear() {
        tools.clear();
    }
}
