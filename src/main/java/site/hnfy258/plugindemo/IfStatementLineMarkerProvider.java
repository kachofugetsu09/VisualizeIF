package site.hnfy258.plugindemo;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

public class IfStatementLineMarkerProvider extends RelatedItemLineMarkerProvider {

    private final Map<PsiMethod, CachedIfTree> ifTreeCache = new WeakHashMap<>();





    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof PsiMethod)) {
            return;
        }

        PsiMethod method = (PsiMethod) element;
        PsiCodeBlock body = method.getBody();
        if (body == null) {
            return;
        }

        // Check if the method has if statements
        boolean hasControlFlowStructures = false;
        for (PsiStatement statement : body.getStatements()) {
            if (containsControlFlowStructure(statement)) {
                hasControlFlowStructures = true;
                break;
            }
        }

        if (!hasControlFlowStructures) {
            return;
        }

        PsiElement nameIdentifier = method.getNameIdentifier();
        if (nameIdentifier == null) {
            return;
        }

        // Create click event handler
        GutterIconNavigationHandler<PsiElement> handler = (e, elt) -> {
            showIfTreePopup(e, method);
        };

        // 创建图标标记
        RelatedItemLineMarkerInfo<PsiElement> info = NavigationGutterIconBuilder
                .create(AllIcons.General.InspectionsEye)
                .setTargets(method)
                .setTooltipText("查看控制流结构")
                .setPopupTitle("控制流结构")
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setNamer(methodElement -> "控制流结构")
                .createLineMarkerInfo(nameIdentifier, handler);

        result.add(info);
    }

    /**
     * 检查语句中是否包含任何控制流结构
     */
    private boolean containsControlFlowStructure(PsiStatement statement) {
        if (statement instanceof PsiIfStatement ||
                statement instanceof PsiWhileStatement ||
                statement instanceof PsiDoWhileStatement ||
                statement instanceof PsiForStatement ||
                statement instanceof PsiForeachStatement ||
                statement instanceof PsiSwitchStatement ||
                statement instanceof PsiTryStatement) {
            return true;
        }

        if (statement instanceof PsiBlockStatement) {
            PsiCodeBlock block = ((PsiBlockStatement) statement).getCodeBlock();
            for (PsiStatement childStatement : block.getStatements()) {
                if (containsControlFlowStructure(childStatement)) {
                    return true;
                }
            }
        }

        return false;
    }

    private IFTreeNode getIfTree(PsiMethod method) {
        Project project = method.getProject();
        PsiFile containingFile = method.getContainingFile();
        long currentModificationStamp = containingFile.getModificationStamp();

        CachedIfTree cachedTree = ifTreeCache.get(method);
        if (cachedTree != null && cachedTree.modificationStamp == currentModificationStamp) {
            return cachedTree.tree;
        }

        // 缓存不存在或已过期，重新分析
        AnalyzeIf analyzer = new AnalyzeIf();
        IFTreeNode tree = analyzer.analyze(method);
        ifTreeCache.put(method, new CachedIfTree(tree, currentModificationStamp));
        return tree;
    }



    private void showIfTreePopup(MouseEvent e, PsiMethod method) {
        // 创建加载提示
        JBLabel loadingLabel = new JBLabel("正在分析IF结构...");
        JBPopup loadingPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(loadingLabel, loadingLabel)
                .setTitle("分析中")
                .createPopup();
        loadingPopup.show(new RelativePoint(e));

        // 后台线程执行分析
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            IFTreeNode ifTree = getIfTree(method);

            // 切回UI线程显示结果
            ApplicationManager.getApplication().invokeLater(() -> {
                loadingPopup.cancel();
                showResultPopup(e, method, ifTree);
            });
        });
    }


    private void showResultPopup(MouseEvent e, PsiMethod method, IFTreeNode ifTree) {
        // 获取IDE编辑器字体
        Font editorFont = EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN);

        JPanel panel = new JPanel(new BorderLayout());

        // 使用JEditorPane显示HTML内容
        JEditorPane editorPane = new JEditorPane("text/html", "");
        editorPane.setEditable(false);
        editorPane.setFont(editorFont);
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        // 获取IDE配色方案
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        Color backgroundColor = scheme.getDefaultBackground();
        Color foregroundColor = scheme.getDefaultForeground();

        editorPane.setBackground(backgroundColor);
        editorPane.setForeground(foregroundColor);
        editorPane.setBorder(JBUI.Borders.empty(10));

        String htmlContent = "<html><body style='font-family: monospace;'><pre>" +
                ifTree.toStringCached() +
                "</pre></body></html>";
        editorPane.setText(htmlContent);

        JBScrollPane scrollPane = new JBScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        scrollPane.setBorder(JBUI.Borders.empty());

        panel.add(scrollPane, BorderLayout.CENTER);

        JDialog dialog = new JDialog();
        dialog.setTitle("IF Logic Structure - " + method.getName());
        dialog.setContentPane(panel);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(null);


        // 显示对话框
        dialog.setVisible(true);
    }
}

