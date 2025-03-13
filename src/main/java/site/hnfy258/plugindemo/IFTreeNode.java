package site.hnfy258.plugindemo;

import java.awt.*;
import java.util.*;
import java.util.List;

public class IFTreeNode {
    public enum NodeType {
        IF, ELSE_IF, ELSE, THEN_BRANCH, STATEMENT, METHOD,
        WHILE, DO_WHILE, FOR, SWITCH, CASE, DEFAULT, TRY,
        CATCH, FINALLY,SWITCH_EXPR,EXPRESSION, TEMP, RESOURCE
    }

    private NodeType type;
    private String text;
    // 懒加载子节点列表，只在需要时初始化
    private List<IFTreeNode> children;


    // 在类的顶部添加这些字段
    private static final Color[] PREDEFINED_COLORS = {
            new Color(255, 100, 100),  // 亮红色
            new Color(100, 255, 100),  // 亮绿色
            new Color(100, 180, 255),  // 亮蓝色
            new Color(255, 200, 100),  // 亮橙色
            new Color(200, 100, 255),  // 亮紫色
            new Color(100, 255, 200),  // 亮青色
            new Color(255, 130, 200),  // 亮粉色
            new Color(180, 255, 100),  // 亮黄绿色
            new Color(255, 255, 150),  // 亮黄色
            new Color(150, 255, 255)   // 亮蓝绿色
    };
    private static final Random random = new Random();

    // 添加此方法用于获取深度对应的颜色
    // 添加一个颜色缓存，用于存储超过预定义颜色数量的层级颜色
    private static final Map<Integer, Color> colorCache = new HashMap<>();

    /**
     * 获取指定深度的颜色
     */
    private Color getColorForDepth(int depth) {
        if (depth < PREDEFINED_COLORS.length) {
            return PREDEFINED_COLORS[depth];
        }

        // 检查缓存中是否已有该深度的颜色
        if (colorCache.containsKey(depth)) {
            return colorCache.get(depth);
        }

        // 生成新的随机颜色（确保在黑色背景上可见）
        Random random = new Random(depth);
        int r = 100 + random.nextInt(156); // 100-255范围，确保足够亮
        int g = 100 + random.nextInt(156);
        int b = 100 + random.nextInt(156);
        Color newColor = new Color(r, g, b);

        // 将新颜色存入缓存
        colorCache.put(depth, newColor);

        return newColor;
    }


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
        StringBuilder builder = new StringBuilder(256);
        buildString(builder, "", "", 0); // 添加深度参数
        return builder.toString();
    }

    private void buildString(StringBuilder builder, String prefix, String childrenPrefix, int depth) {
        // 当前节点使用当前深度的颜色
        Color nodeColor = getColorForDepth(depth);
        String colorHtml = String.format("<span style=\"color:rgb(%d,%d,%d)\">",
                nodeColor.getRed(), nodeColor.getGreen(), nodeColor.getBlue());
        String closeSpan = "</span>";

        builder.append(prefix).append(colorHtml).append(escapeHtml(text)).append(closeSpan).append('\n');

        if (children == null) return;

        final int lastIndex = children.size() - 1;
        for (int i = 0; i < children.size(); i++) {
            IFTreeNode child = children.get(i);
            boolean isLast = (i == lastIndex);

            // 子节点的颜色 - 用于连线
            Color childColor = getColorForDepth(depth + 1);
            String childColorHtml = String.format("<span style=\"color:rgb(%d,%d,%d)\">",
                    childColor.getRed(), childColor.getGreen(), childColor.getBlue());

            // 连接线使用子节点的颜色
            String newPrefix = childrenPrefix + (isLast ?
                    childColorHtml + "└── " + closeSpan :
                    childColorHtml + "├── " + closeSpan);
            String newChildrenPrefix = childrenPrefix + (isLast ?
                    "    " :
                    childColorHtml + "│   " + closeSpan);

            child.buildString(builder, newPrefix, newChildrenPrefix, depth + 1);
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
