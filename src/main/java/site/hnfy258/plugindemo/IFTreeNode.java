package site.hnfy258.plugindemo;

import java.util.ArrayList;
import java.util.List;

public class IFTreeNode {
    public enum NodeType {
        IF, ELSE_IF, ELSE, THEN_BRANCH, STATEMENT, METHOD
    }

    private NodeType type;
    private String text;
    private List<IFTreeNode> children;

    public IFTreeNode(NodeType type, String text) {
        this.type = type;
        this.text = text;
        this.children = new ArrayList<>();
    }

    public void addChild(IFTreeNode child) {
        children.add(child);
    }

    public NodeType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public List<IFTreeNode> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        buildString(builder, "", "");
        return builder.toString();
    }

    private void buildString(StringBuilder builder, String prefix, String childrenPrefix) {
        builder.append(prefix);
        builder.append(text);
        builder.append("\n");

        for (int i = 0; i < children.size(); i++) {
            IFTreeNode child = children.get(i);
            String newPrefix;
            String newChildrenPrefix;

            if (i < children.size() - 1) {
                // 非最后一个子节点
                newPrefix = childrenPrefix + "├── ";
                newChildrenPrefix = childrenPrefix + "│   ";
            } else {
                // 最后一个子节点
                newPrefix = childrenPrefix + "└── ";
                newChildrenPrefix = childrenPrefix + "    ";
            }

            child.buildString(builder, newPrefix, newChildrenPrefix);
        }
    }
}

