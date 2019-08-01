// This is a generated file. Not intended for manual editing.
package com.intellij.idea.plugin.hybris.flexibleSearch.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface FlexibleSearchValueExpression extends PsiElement {

  @Nullable
  FlexibleSearchColumnLocalization getColumnLocalization();

  @Nullable
  FlexibleSearchColumnReference getColumnReference();

  @Nullable
  FlexibleSearchParameterReference getParameterReference();

  @Nullable
  PsiElement getLeftBrace();

  @Nullable
  PsiElement getNumber();

  @Nullable
  PsiElement getRightBrace();

}
