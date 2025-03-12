package site.hnfy258.plugindemo;

import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.List;

public class AnalyzeIf {
    public IFTreeNode analyze(PsiMethod method) {
        String methodName = method.getName() + getParameterList(method);
        IFTreeNode rootNode = new IFTreeNode(IFTreeNode.NodeType.METHOD, methodName);

        PsiCodeBlock body = method.getBody();
        if (body != null) {
            analyzeCodeBlock(body, rootNode);
        }

        return rootNode;
    }

    private String getParameterList(PsiMethod method) {
        StringBuilder params = new StringBuilder("(");
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                params.append(", ");
            }
            params.append(parameters[i].getType().getPresentableText())
                    .append(" ")
                    .append(parameters[i].getName());
        }
        params.append(")");
        return params.toString();
    }

    private void analyzeCodeBlock(PsiCodeBlock codeBlock, IFTreeNode parentNode) {
        for (PsiStatement statement : codeBlock.getStatements()) {
            analyzeStatement(statement, parentNode);
        }
    }

    private void analyzeStatement(PsiStatement statement, IFTreeNode parentNode) {
        if (statement instanceof PsiIfStatement) {
            analyzeIfStatement((PsiIfStatement) statement, parentNode);
        } else if (statement instanceof PsiBlockStatement) {
            analyzeCodeBlock(((PsiBlockStatement) statement).getCodeBlock(), parentNode);
        }
        // Ignore other statement types for this visualization
    }

    private void analyzeIfStatement(PsiIfStatement ifStatement, IFTreeNode parentNode) {
        String condition = ifStatement.getCondition() != null ?
                ifStatement.getCondition().getText() : "no condition";

        IFTreeNode ifNode = new IFTreeNode(IFTreeNode.NodeType.IF, "if (" + condition + ")");
        parentNode.addChild(ifNode);

        // Process then branch
        if (ifStatement.getThenBranch() != null) {
            analyzeBranch(ifStatement.getThenBranch(), ifNode);
        }

        // Process else branch
        if (ifStatement.getElseBranch() != null) {
            if (ifStatement.getElseBranch() instanceof PsiIfStatement) {
                // This is an else-if
                PsiIfStatement elseIfStatement = (PsiIfStatement) ifStatement.getElseBranch();
                String elseIfCondition = elseIfStatement.getCondition() != null ?
                        elseIfStatement.getCondition().getText() : "no condition";

                IFTreeNode elseIfNode = new IFTreeNode(IFTreeNode.NodeType.ELSE_IF,
                        "else if (" + elseIfCondition + ")");
                parentNode.addChild(elseIfNode);

                // Process else-if then branch
                if (elseIfStatement.getThenBranch() != null) {
                    analyzeBranch(elseIfStatement.getThenBranch(), elseIfNode);
                }

                // Process else-if's else branch
                if (elseIfStatement.getElseBranch() != null) {
                    if (elseIfStatement.getElseBranch() instanceof PsiIfStatement) {
                        // Chain of else-if
                        analyzeIfStatement((PsiIfStatement) elseIfStatement.getElseBranch(), parentNode);
                    } else {
                        // Regular else for the else-if
                        IFTreeNode finalElseNode = new IFTreeNode(IFTreeNode.NodeType.ELSE, "else");
                        parentNode.addChild(finalElseNode);
                        analyzeBranch(elseIfStatement.getElseBranch(), finalElseNode);
                    }
                }
            } else {
                // Regular else
                IFTreeNode elseNode = new IFTreeNode(IFTreeNode.NodeType.ELSE, "else");
                parentNode.addChild(elseNode);
                analyzeBranch(ifStatement.getElseBranch(), elseNode);
            }
        }
    }

    private void analyzeBranch(PsiStatement branch, IFTreeNode parentNode) {
        if (branch instanceof PsiBlockStatement) {
            // Block of statements
            PsiCodeBlock codeBlock = ((PsiBlockStatement) branch).getCodeBlock();
            analyzeCodeBlock(codeBlock, parentNode);
        } else if (branch instanceof PsiIfStatement) {
            // Nested if without block (single line if)
            analyzeIfStatement((PsiIfStatement) branch, parentNode);
        } else if (branch instanceof PsiExpressionStatement) {
            // Single expression statement - not important for if tree
        } else {
            // Handle other statement types
            analyzeStatement(branch, parentNode);
        }
    }
}

