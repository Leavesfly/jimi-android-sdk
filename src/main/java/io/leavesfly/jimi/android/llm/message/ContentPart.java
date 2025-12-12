package io.leavesfly.jimi.android.llm.message;

/**
 * 内容部分（简化版）
 */
public class ContentPart {
    
    private final String text;
    
    private ContentPart(String text) {
        this.text = text;
    }
    
    public String getText() {
        return text;
    }
    
    public static ContentPart text(String text) {
        return new ContentPart(text);
    }
    
    public static java.util.List<ContentPart> textList(String text) {
        java.util.List<ContentPart> list = new java.util.ArrayList<>();
        list.add(text(text));
        return list;
    }
}
