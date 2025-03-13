package site.hnfy258.plugindemo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IFTreeNode {
    public enum NodeType {
        IF, ELSE_IF, ELSE, THEN_BRANCH, STATEMENT, METHOD,
        WHILE, DO_WHILE, FOR, SWITCH, CASE, DEFAULT, TRY, CATCH, FINALLY,SWITCH_EXPR,EXPRESSION, RESOURCE
    }

    private NodeType type;
    private String text;
    // 懒加载子节点列表，只在需要时初始化
    private List<IFTreeNode> children;

    public IFTreeNode(NodeType type, String text) {
        this.type = type;
        this.text = text;
        // 不在构造函数中初始化children，而是在需要时才创建
    }

    public void addChild(IFTreeNode child) {
        if (children == null) {
            children = new ArrayList<>(4); // 使用较小的初始容量
        }
        children.add(child);
    }

    public NodeType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public List<IFTreeNode> getChildren() {
        return children == null ? Collections.emptyList() : children;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(256); // 预分配合理大小的缓冲区
        buildString(builder, "", "");
        return builder.toString();
    }

    private void buildString(StringBuilder builder, String prefix, String childrenPrefix) {
        builder.append(prefix).append(text).append('\n');

        if (children == null) return; // 没有子节点时直接返回

        final int lastIndex = children.size() - 1;
        for (int i = 0; i < children.size(); i++) {
            IFTreeNode child = children.get(i);
            boolean isLast = (i == lastIndex);

            String newPrefix = childrenPrefix + (isLast ? "└── " : "├── ");
            String newChildrenPrefix = childrenPrefix + (isLast ? "    " : "│   ");

            child.buildString(builder, newPrefix, newChildrenPrefix);
        }
    }

    // 添加缓存支持
    private String cachedString;

    public String toStringCached() {
        if (cachedString == null) {
            cachedString = toString();
        }
        return cachedString;
    }

    public void invalidateCache() {
        this.cachedString = null;
        // 递归清除所有子节点的缓存
        if (children != null) {
            for (IFTreeNode child : children) {
                child.invalidateCache();
            }
        }
    }
}
