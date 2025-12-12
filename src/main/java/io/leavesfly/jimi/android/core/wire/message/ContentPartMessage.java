package io.leavesfly.jimi.android.core.wire.message;

/**
 * 内容部分消息（流式输出）
 */
public class ContentPartMessage extends WireMessage {
    
    public enum ContentType {
        NORMAL,      // 普通内容
        REASONING    // 推理内容
    }
    
    private final String content;
    private final ContentType contentType;
    
    public ContentPartMessage(String content, ContentType contentType) {
        this.content = content;
        this.contentType = contentType;
    }
    
    public ContentPartMessage(String content) {
        this(content, ContentType.NORMAL);
    }
    
    @Override
    public String getType() {
        return "content_part";
    }
    
    public String getContent() {
        return content;
    }
    
    public ContentType getContentType() {
        return contentType;
    }
}
