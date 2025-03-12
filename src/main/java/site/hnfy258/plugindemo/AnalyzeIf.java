package site.hnfy258.plugindemo;

public class AnalyzeIf {
    //1.遍历方法体(methodBody)，识别所有的条件语句结构
    //2.遇到第一个条件语句(if)时，创建一个新的IFTreeNode作为根节点，将该条件表达式内容作为root的val值
    //3.继续解析代码块，直到遇到对应的结束标记(}或return、break、continue等关键词)
    //   如果在解析过程中遇到else if或else语句，则创建对应的TreeNode，并将其添加为上一个节点的子节点
    //   如果遇到嵌套的if语句，则创建新的TreeNode，并将其添加为当前语句块节点的子节点
    //4.当一个完整的if-else if-else结构解析完成后，继续遍历直到方法体结束
    //   如果遇到新的独立if语句，则创建新的树结构表示该逻辑分支
}