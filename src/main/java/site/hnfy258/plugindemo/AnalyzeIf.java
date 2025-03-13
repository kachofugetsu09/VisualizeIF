package site.hnfy258.plugindemo;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

    // 使用IDEA调度器并行分析多个方法
    public List<IFTreeNode> analyzeMultipleMethods(List<PsiMethod> methods) {
        List<CompletableFuture<IFTreeNode>> futures = new ArrayList<>(methods.size());

        // 使用IDE的线程池提交任务
        for (PsiMethod method : methods) {
            CompletableFuture<IFTreeNode> future = new CompletableFuture<>();

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    IFTreeNode result = analyze(method);
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

            futures.add(future);
        }

        // 收集结果
        List<IFTreeNode> results = new ArrayList<>(methods.size());
        for (CompletableFuture<IFTreeNode> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    // 在UI线程中处理异常
                    // 可以考虑显示通知或记录日志
                    e.printStackTrace();
                });
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
        List<PsiStatement> statements = new ArrayList<>();
        for (PsiStatement statement : codeBlock.getStatements()) {
            statements.add(statement);
        }

        // 对于复杂代码块，考虑使用IDEA的任务调度
        if (statements.size() > 3) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (PsiStatement statement : statements) {
                if (statement instanceof PsiIfStatement) {
                    // 仅对复杂的if语句使用并行处理
                    PsiIfStatement ifStatement = (PsiIfStatement) statement;
                    CompletableFuture<Void> future = new CompletableFuture<>();

                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            analyzeIfStatement(ifStatement, parentNode);
                            future.complete(null);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    });

                    futures.add(future);
                } else {
                    // 其他语句直接处理
                    analyzeStatement(statement, parentNode);
                }
            }

            // 等待所有并行任务完成
            for (CompletableFuture<Void> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            // 语句较少时顺序处理
            for (PsiStatement statement : statements) {
                analyzeStatement(statement, parentNode);
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
        } else if (statement instanceof PsiExpressionStatement) {
        }
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
    }

    /**
     * 分析while循环语句
     */
    private void analyzeWhileStatement(PsiWhileStatement whileStatement, IFTreeNode parentNode) {
        // TODO:
        // 1. 提取循环条件
        String condition = whileStatement.getCondition() != null ?
                whileStatement.getCondition().getText() : "no condition";
        // 2. 创建WHILE类型节点
        IFTreeNode whileNode = new IFTreeNode(IFTreeNode.NodeType.WHILE, "while (" + condition + ")");
        // 3. 添加到父节点
        parentNode.addChild(whileNode);
        // 4. 分析循环体内容
        analyzeBranch(whileStatement.getBody(), whileNode);

    }

    /**
     * 分析do-while循环语句
     */
    private void analyzeDoWhileStatement(PsiDoWhileStatement doWhileStatement, IFTreeNode parentNode) {
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
    }

    /**
     * 分析for循环语句
     */
    private void analyzeForStatement(PsiForStatement forStatement, IFTreeNode parentNode) {
        // 1. 提取初始化语句、条件和更新语句
        StringBuilder forText = new StringBuilder("for (");

        // 初始化部分
        if (forStatement.getInitialization() != null) {
            forText.append(forStatement.getInitialization().getText());
        }
        forText.append("; ");

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
    }

    /**
     * 分析增强型for循环语句
     */
    private void analyzeForeachStatement(PsiForeachStatement foreachStatement, IFTreeNode parentNode) {
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
    }

    /**
     * 分析switch语句
     */
    private void analyzeSwitchStatement(PsiSwitchStatement switchStatement, IFTreeNode parentNode) {
        //TODO
    }

    /**
     * 分析switch表达式（Java 12+）
     */
    private void analyzeSwitchExpression(PsiSwitchExpression switchExpression, IFTreeNode parentNode) {
        //TODO
    }

    /**
     * 分析try-catch-finally语句
     */
    private void analyzeTryStatement(PsiTryStatement tryStatement, IFTreeNode parentNode) {
        //TODO
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
        } else {
            analyzeStatement(branch, parentNode);
        }
    }


}

