package site.hnfy258.plugindemo;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collection;

public class IfStatementLineMarkerProvider extends RelatedItemLineMarkerProvider {
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
        boolean hasIfStatements = false;
        for (PsiStatement statement : body.getStatements()) {
            if (containsIfStatement(statement)) {
                hasIfStatements = true;
                break;
            }
        }

        if (!hasIfStatements) {
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

        // Create icon marker
        RelatedItemLineMarkerInfo<PsiElement> info = NavigationGutterIconBuilder
                .create(AllIcons.General.InspectionsEye)
                .setTargets(method)
                .setTooltipText("View IF Logic Structure")
                .setPopupTitle("IF Logic Structure")
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setNamer(methodElement -> "IF Logic Structure")
                .createLineMarkerInfo(nameIdentifier, handler);

        result.add(info);
    }

    private boolean containsIfStatement(PsiStatement statement) {
        if (statement instanceof PsiIfStatement) {
            return true;
        }

        if (statement instanceof PsiBlockStatement) {
            PsiCodeBlock block = ((PsiBlockStatement) statement).getCodeBlock();
            for (PsiStatement childStatement : block.getStatements()) {
                if (containsIfStatement(childStatement)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void showIfTreePopup(MouseEvent e, PsiMethod method) {
        Project project = method.getProject();
        AnalyzeIf analyzer = new AnalyzeIf();
        IFTreeNode ifTree = analyzer.analyze(method);

        // Get IDE's editor font
        Font editorFont = EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN);

        // Create text area to display the IF tree
        JBTextArea textArea = new JBTextArea(ifTree.toString());
        textArea.setEditable(false);

        // Use the IDE's editor font
        textArea.setFont(editorFont);

        // Get IDE color scheme
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        Color backgroundColor = scheme.getDefaultBackground();
        Color foregroundColor = scheme.getDefaultForeground();

        textArea.setBackground(backgroundColor);
        textArea.setForeground(foregroundColor);
        textArea.setBorder(JBUI.Borders.empty(10));

        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        scrollPane.setBorder(JBUI.Borders.empty());

        // Create popup window
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scrollPane, textArea)
                .setTitle("IF Logic Structure - " + method.getName())
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .createPopup();

        // Show popup window
        popup.show(new RelativePoint(e));
    }
}

