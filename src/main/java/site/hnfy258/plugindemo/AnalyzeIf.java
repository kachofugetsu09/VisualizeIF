package site.hnfy258.plugindemo;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AnalyzeIf {
    public IFTreeNode analyze(PsiMethod method) {
        return ReadAction.compute(() -> {
            String methodName = method.getName() + getParameterList(method);
            IFTreeNode rootNode = new IFTreeNode(IFTreeNode.NodeType.METHOD, methodName);

            PsiCodeBlock body = method.getBody();
            if (body != null) {
                analyzeCodeBlock(body, rootNode);
            }

            return rootNode;
        });
    }

    // 使用IDEA调度器并行分析多个方法
    public List<IFTreeNode> analyzeMultipleMethods(List<PsiMethod> methods) {
        List<CompletableFuture<IFTreeNode>> futures = new ArrayList<>(methods.size());
        // 创建固定大小的结果数组，用于保持顺序
        IFTreeNode[] orderedResults = new IFTreeNode[methods.size()];

        // 使用IDE的线程池提交任务
        for (int i = 0; i < methods.size(); i++) {
            final PsiMethod method = methods.get(i);
            final int methodIndex = i;

            CompletableFuture<IFTreeNode> future = new CompletableFuture<>();

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    IFTreeNode result = analyze(method);
                    // 直接将结果放入正确的位置
                    orderedResults[methodIndex] = result;
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

            futures.add(future);
        }

        // 等待所有任务完成
        for (CompletableFuture<IFTreeNode> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    // 在UI线程中处理异常
                    e.printStackTrace();
                });
            }
        }

        // 将有序结果转换为List
        List<IFTreeNode> results = new ArrayList<>(methods.size());
        for (IFTreeNode node : orderedResults) {
            if (node != null) {
                results.add(node);
            }
        }

        return results;
    }

    public void analyzeMultipleMethodsWithProgress(List<PsiMethod> methods, Runnable onComplete) {
        ProgressManager.getInstance().run(new Task.Backgroundable(
                null, "Analyzing Methods", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                List<IFTreeNode> results = new ArrayList<>(methods.size());

                for (int i = 0; i < methods.size(); i++) {
                    if (indicator.isCanceled()) {
                        break;
                    }

                    indicator.setText("Analyzing method: " + methods.get(i).getName());
                    indicator.setFraction((double) i / methods.size());

                    results.add(analyze(methods.get(i)));
                }

                // 在UI线程中处理结果
                ApplicationManager.getApplication().invokeLater(() -> {
                    // 这里可以处理结果
                    onComplete.run();
                });
            }
        });
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
        // Use ReadAction to safely gather all statements from the code block
        List<PsiStatement> statements = ReadAction.compute(() -> {
            List<PsiStatement> result = new ArrayList<>();
            for (PsiStatement statement : codeBlock.getStatements()) {
                result.add(statement);
            }
            return result;
        });

        // For complex code blocks, consider using IDEA's task scheduling
        if (statements.size() > 3) {
            // Create a result list with the same size as the statements list, preserving original order
            List<IFTreeNode[]> resultNodes = new ArrayList<>(statements.size());
            for (int i = 0; i < statements.size(); i++) {
                resultNodes.add(null);
            }

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (int i = 0; i < statements.size(); i++) {
                final PsiStatement statement = statements.get(i);
                final int statementIndex = i;

                if (statement instanceof PsiIfStatement) {
                    // Only use parallel processing for complex if statements
                    PsiIfStatement ifStatement = (PsiIfStatement) statement;
                    CompletableFuture<Void> future = new CompletableFuture<>();

                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            // Create a temporary parent node to collect results
                            IFTreeNode tempParent = new IFTreeNode(IFTreeNode.NodeType.TEMP, "temp");

                            // Wrap the PSI access in ReadAction
                            ReadAction.run(() -> analyzeIfStatement(ifStatement, tempParent));

                            // Store the child node(s) of the temporary node to add in original order
                            resultNodes.set(statementIndex, tempParent.getChildren().toArray(new IFTreeNode[0]));
                            future.complete(null);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    });

                    futures.add(future);
                } else {
                    // Process other statements directly
                    // Create a temporary parent node
                    IFTreeNode tempParent = new IFTreeNode(IFTreeNode.NodeType.TEMP, "temp");

                    // Wrap the PSI access in ReadAction
                    ReadAction.run(() -> analyzeStatement(statement, tempParent));

                    resultNodes.set(statementIndex, tempParent.getChildren().toArray(new IFTreeNode[0]));
                }
            }

            // Wait for all parallel tasks to complete
            for (CompletableFuture<Void> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Add results to the parent node in original order
            for (IFTreeNode[] nodes : resultNodes) {
                if (nodes != null) {
                    for (IFTreeNode node : nodes) {
                        parentNode.addChild(node);
                    }
                }
            }
        } else {
            // Process sequentially for fewer statements
            for (PsiStatement statement : statements) {
                // Wrap the PSI access in ReadAction
                ReadAction.run(() -> analyzeStatement(statement, parentNode));
            }
        }
    }

    private void analyzeStatement(PsiStatement statement, IFTreeNode parentNode) {
        if (statement instanceof PsiIfStatement) {
            analyzeIfStatement((PsiIfStatement) statement, parentNode);
        } else if (statement instanceof PsiWhileStatement) {
            analyzeWhileStatement((PsiWhileStatement) statement, parentNode);
        } else if (statement instanceof PsiDoWhileStatement) {
            analyzeDoWhileStatement((PsiDoWhileStatement) statement, parentNode);
        } else if (statement instanceof PsiForStatement) {
            analyzeForStatement((PsiForStatement) statement, parentNode);
        } else if (statement instanceof PsiForeachStatement) {
            analyzeForeachStatement((PsiForeachStatement) statement, parentNode);
        } else if (statement instanceof PsiSwitchStatement) {
            analyzeSwitchStatement((PsiSwitchStatement) statement, parentNode);
        } else if (statement instanceof PsiTryStatement) {
            analyzeTryStatement((PsiTryStatement) statement, parentNode);
        } else if (statement instanceof PsiBlockStatement) {
            analyzeCodeBlock(((PsiBlockStatement) statement).getCodeBlock(), parentNode);
        }
    }


    private void analyzeIfStatement(PsiIfStatement ifStatement, IFTreeNode parentNode) {
        ReadAction.run(()->
        {
            String condition = ifStatement.getCondition() != null ?
                    ifStatement.getCondition().getText() : "no condition";

            IFTreeNode ifNode = new IFTreeNode(IFTreeNode.NodeType.IF, "if (" + condition + ")");
            parentNode.addChild(ifNode);

            // Process then branch
            if (ifStatement.getThenBranch() != null) {
                analyzeBranch(ifStatement.getThenBranch(), ifNode);
            }

            if (ifStatement.getElseBranch() != null) {
                if (ifStatement.getElseBranch() instanceof PsiIfStatement) {
                    PsiIfStatement elseIfStatement = (PsiIfStatement) ifStatement.getElseBranch();
                    String elseIfCondition = elseIfStatement.getCondition() != null ?
                            elseIfStatement.getCondition().getText() : "no condition";

                    IFTreeNode elseIfNode = new IFTreeNode(IFTreeNode.NodeType.ELSE_IF,
                            "else if (" + elseIfCondition + ")");
                    parentNode.addChild(elseIfNode);

                    if (elseIfStatement.getThenBranch() != null) {
                        analyzeBranch(elseIfStatement.getThenBranch(), elseIfNode);
                    }

                    if (elseIfStatement.getElseBranch() != null) {
                        if (elseIfStatement.getElseBranch() instanceof PsiIfStatement) {
                            analyzeIfStatement((PsiIfStatement) elseIfStatement.getElseBranch(), parentNode);
                        } else {
                            IFTreeNode finalElseNode = new IFTreeNode(IFTreeNode.NodeType.ELSE, "else");
                            parentNode.addChild(finalElseNode);
                            analyzeBranch(elseIfStatement.getElseBranch(), finalElseNode);
                        }
                    }
                } else {
                    IFTreeNode elseNode = new IFTreeNode(IFTreeNode.NodeType.ELSE, "else");
                    parentNode.addChild(elseNode);
                    analyzeBranch(ifStatement.getElseBranch(), elseNode);
                }
            }
        });
    }

    /**
     * 分析while循环语句
     */
    private void analyzeWhileStatement(PsiWhileStatement whileStatement, IFTreeNode parentNode) {
        ReadAction.run(()->{
            // 1. 提取循环条件
            String condition = whileStatement.getCondition() != null ?
                    whileStatement.getCondition().getText() : "no condition";
            // 2. 创建WHILE类型节点
            IFTreeNode whileNode = new IFTreeNode(IFTreeNode.NodeType.WHILE, "while (" + condition + ")");
            // 3. 添加到父节点
            parentNode.addChild(whileNode);
            // 4. 分析循环体内容
            analyzeBranch(whileStatement.getBody(), whileNode);

        });

    }

    /**
     * 分析do-while循环语句
     */
    private void analyzeDoWhileStatement(PsiDoWhileStatement doWhileStatement, IFTreeNode parentNode) {
        ReadAction.run(()->{
            // 1. 提取循环条件
            String condition = doWhileStatement.getCondition() != null ?
                    doWhileStatement.getCondition().getText() : "no condition";

            // 2. 创建DO_WHILE类型节点
            IFTreeNode doWhileNode = new IFTreeNode(IFTreeNode.NodeType.DO_WHILE, "do-while (" + condition + ")");

            // 3. 添加到父节点
            parentNode.addChild(doWhileNode);

            // 4. 分析循环体内容
            if (doWhileStatement.getBody() != null) {
                analyzeBranch(doWhileStatement.getBody(), doWhileNode);
            }
        });

    }

    /**
     * 分析for循环语句
     */
    private void analyzeForStatement(PsiForStatement forStatement, IFTreeNode parentNode) {
        ReadAction.run(()->{
            // 1. 提取初始化语句、条件和更新语句
            StringBuilder forText = new StringBuilder("for (");

            // 初始化部分
            if (forStatement.getInitialization() != null) {
                forText.append(forStatement.getInitialization().getText());
            }

            // 条件部分
            if (forStatement.getCondition() != null) {
                forText.append(forStatement.getCondition().getText());
            }
            forText.append("; ");

            // 更新部分
            if (forStatement.getUpdate() != null) {
                forText.append(forStatement.getUpdate().getText());
            }
            forText.append(")");

            // 2. 创建FOR类型节点
            IFTreeNode forNode = new IFTreeNode(IFTreeNode.NodeType.FOR, forText.toString());

            // 3. 添加到父节点
            parentNode.addChild(forNode);

            // 4. 分析循环体内容
            if (forStatement.getBody() != null) {
                analyzeBranch(forStatement.getBody(), forNode);
            }
        });

    }

    /**
     * 分析增强型for循环语句
     */
    private void analyzeForeachStatement(PsiForeachStatement foreachStatement, IFTreeNode parentNode) {
        ReadAction.run(()->{
            // 1. 提取迭代变量和集合表达式
            StringBuilder foreachText = new StringBuilder("for (");

            // 迭代变量
            if (foreachStatement.getIterationParameter() != null) {
                PsiParameter param = foreachStatement.getIterationParameter();
                foreachText.append(param.getType().getPresentableText())
                        .append(" ")
                        .append(param.getName());
            }

            foreachText.append(" : ");

            // 集合表达式
            if (foreachStatement.getIteratedValue() != null) {
                foreachText.append(foreachStatement.getIteratedValue().getText());
            }

            foreachText.append(")");

            // 2. 创建FOR类型节点并特别标记为foreach
            IFTreeNode foreachNode = new IFTreeNode(IFTreeNode.NodeType.FOR, foreachText.toString());

            // 3. 添加到父节点
            parentNode.addChild(foreachNode);

            // 4. 分析循环体内容
            if (foreachStatement.getBody() != null) {
                analyzeBranch(foreachStatement.getBody(), foreachNode);
            }
        });

    }


    /**
     * 分析switch语句
     */
    private void analyzeSwitchStatement(PsiSwitchStatement switchStatement, IFTreeNode parentNode) {
        ReadAction.run(() -> {
            // 获取switch语句的选择器表达式
            String expressionText = switchStatement.getExpression() != null ?
                    switchStatement.getExpression().getText() : "no expression";

            String switchText = "switch (" + expressionText + ")";
            IFTreeNode switchNode = new IFTreeNode(IFTreeNode.NodeType.SWITCH, switchText);
            parentNode.addChild(switchNode);

            PsiCodeBlock body = switchStatement.getBody();
            if (body != null) {
                for (PsiSwitchLabelStatement label : PsiTreeUtil.findChildrenOfType(body, PsiSwitchLabelStatement.class)) {
                    String caseText;
                    if (label.isDefaultCase()) {
                        caseText = "default:";
                    } else {
                        caseText = label.getText();
                    }

                    IFTreeNode caseNode = new IFTreeNode(IFTreeNode.NodeType.CASE, caseText);
                    switchNode.addChild(caseNode);

                    PsiElement current = label.getNextSibling();
                    while (current != null && !(current instanceof PsiSwitchLabelStatement)) {
                        if (current instanceof PsiStatement) {
                            analyzeStatement((PsiStatement) current, caseNode);
                        }
                        current = current.getNextSibling();
                    }
                }

                // 处理增强型switch语句(使用->的语法)
                for (PsiSwitchLabeledRuleStatement rule : PsiTreeUtil.findChildrenOfType(body, PsiSwitchLabeledRuleStatement.class)) {
                    String caseText;
                    if (rule.isDefaultCase()) {
                        caseText = "default";
                    } else {
                        PsiCaseLabelElementList labelList = rule.getCaseLabelElementList();
                        if (labelList != null) {
                            caseText = "case " + labelList.getText();
                        } else {
                            caseText = rule.getText();
                        }
                    }

                    IFTreeNode caseNode = new IFTreeNode(IFTreeNode.NodeType.CASE, caseText);
                    switchNode.addChild(caseNode);

                    // 分析case分支的主体
                    PsiStatement ruleBody = rule.getBody();
                    if (ruleBody != null) {
                        analyzeStatement(ruleBody, caseNode);
                    }
                }
            }
        });
    }





    /**
     * 分析try-catch-finally语句
     */
    private void analyzeTryStatement(PsiTryStatement tryStatement, IFTreeNode parentNode) {
        ReadAction.run(()->{
            String tryText = "try";
            if(tryStatement.getResourceList() != null){
                tryText += tryStatement.getResourceList().getText();
            }
            IFTreeNode tryNode = new IFTreeNode(IFTreeNode.NodeType.TRY, tryText);
            parentNode.addChild(tryNode);

            // Analyze try block
            PsiCodeBlock tryBlock = tryStatement.getTryBlock();
            if (tryBlock != null) {
                for (PsiStatement statement : tryBlock.getStatements()) {
                    analyzeStatement(statement, tryNode);
                }
            }

            PsiCatchSection[] catchSections = tryStatement.getCatchSections();
            for (PsiCatchSection catchSection : catchSections) {
                PsiParameter parameter = catchSection.getParameter();
                if (parameter != null) {
                    String catchText = "catch (" + parameter.getType().getPresentableText() + " " + parameter.getName() + ")";
                    IFTreeNode catchNode = new IFTreeNode(IFTreeNode.NodeType.CATCH, catchText);
                    parentNode.addChild(catchNode);

                    PsiCodeBlock catchBlock = catchSection.getCatchBlock();
                    if (catchBlock != null) {
                        for (PsiStatement statement : catchBlock.getStatements()) {
                            analyzeStatement(statement, catchNode);
                        }
                    }
                }
            }


            PsiCodeBlock finallyBlock = tryStatement.getFinallyBlock();
            if (finallyBlock != null) {
                IFTreeNode finallyNode = new IFTreeNode(IFTreeNode.NodeType.FINALLY, "finally");
                parentNode.addChild(finallyNode);  // Add to parent, not tryNode

                for (PsiStatement statement : finallyBlock.getStatements()) {
                    analyzeStatement(statement, finallyNode);
                }
            }
        });

    }


    private void analyzeBranch(PsiStatement branch, IFTreeNode parentNode) {
        if (branch instanceof PsiBlockStatement) {
            PsiCodeBlock codeBlock = ((PsiBlockStatement) branch).getCodeBlock();
            analyzeCodeBlock(codeBlock, parentNode);
        } else if (branch instanceof PsiIfStatement) {
            analyzeIfStatement((PsiIfStatement) branch, parentNode);
        } else if (branch instanceof PsiExpressionStatement) {
        } else if (branch instanceof PsiWhileStatement) {
            analyzeWhileStatement((PsiWhileStatement) branch, parentNode);
        } else if (branch instanceof PsiForStatement) {
            analyzeForStatement((PsiForStatement) branch, parentNode);
        } else if (branch instanceof PsiForeachStatement) {
            analyzeForeachStatement((PsiForeachStatement) branch, parentNode);
        } else if (branch instanceof PsiDoWhileStatement) {
            analyzeDoWhileStatement((PsiDoWhileStatement) branch, parentNode);
        } else if (branch instanceof PsiSwitchStatement) {
            analyzeSwitchStatement((PsiSwitchStatement) branch, parentNode);
        } else if (branch instanceof  PsiTryStatement) {
            analyzeTryStatement((PsiTryStatement) branch, parentNode);

        } else {
            analyzeStatement(branch, parentNode);
        }
    }


}

