package site.hnfy258.plugindemo;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.IconLoader;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

public class IfStatementLineMarkerProvider extends RelatedItemLineMarkerProvider {
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result){
        if(!(element instanceof PsiMethod )){
            return;
        }
        PsiMethod method = (PsiMethod) element;
        PsiCodeBlock body = method.getBody();
        if(body==null){
            return;
        }
        PsiIfStatement[] ifStatements = new PsiIfStatement[]{PsiTreeUtil.findChildOfAnyType(body, PsiIfStatement.class)};
        if(ifStatements.length<=0){
            return;
        }
        PsiElement nameIdentifier = method.getNameIdentifier();
        if(nameIdentifier==null){
            return;
        }
        NavigationGutterIconBuilder<PsiElement> builder =
                NavigationGutterIconBuilder.create(AllIcons.General.BalloonInformation).
                        setTargets(ifStatements).setTooltipText("存在If逻辑").setPopupTitle("If逻辑").setAlignment(GutterIconRenderer.Alignment.RIGHT);
        result.add(builder.createLineMarkerInfo(nameIdentifier));
    }
}
