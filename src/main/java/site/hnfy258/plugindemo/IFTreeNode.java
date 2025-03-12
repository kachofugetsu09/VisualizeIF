package site.hnfy258.plugindemo;

import java.util.ArrayList;
import java.util.List;

/**
 * 表示条件逻辑树中的节点
 * 用于IDE插件中可视化展示if语句的逻辑结构
 */
public class IFTreeNode {
    // 节点类型枚举
    public enum NodeType {
        IF,             // if节点
        ELSE_IF,        // else if节点
        ELSE,           // else节点
        CONDITION,      // 条件表达式
        STATEMENT_BLOCK // 语句块
    }

    private String val;              // 条件表达式内容
    private List<IFTreeNode> children; // 子节点列表
    private IFTreeNode parent;       // 父节点
    private NodeType nodeType;       // 节点类型
    private int startLine;           // 在源代码中的起始行
    private int endLine;             // 在源代码中的结束行
    private String sourceCode;       // 源代码片段

    /**
     * 构造函数
     * @param nodeType 节点类型
     * @param val 条件表达式
     */
    public IFTreeNode(NodeType nodeType, String val) {
        this.nodeType = nodeType;
        this.val = val;
        this.children = new ArrayList<>();
    }

}
