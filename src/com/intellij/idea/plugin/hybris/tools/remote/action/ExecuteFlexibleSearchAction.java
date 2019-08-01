/*
 * This file is part of "hybris integration" plugin for Intellij IDEA.
 * Copyright (C) 2014-2016 Alexander Bartash <AlexanderBartash@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.intellij.idea.plugin.hybris.tools.remote.action;

import com.intellij.idea.plugin.hybris.flexibleSearch.file.FlexibleSearchFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Nosov Aleksandr <nosovae.dev@gmail.com>
 */
public class ExecuteFlexibleSearchAction extends AbstractExecuteAction {


    @Override
    public void actionPerformed(@NotNull final AnActionEvent actionEvent) {
        super.actionPerformed(actionEvent);
    }

    @Override
    public void update(@NotNull final AnActionEvent actionEvent) {
        super.update(actionEvent);
        final VirtualFile file = actionEvent.getDataContext().getData(CommonDataKeys.VIRTUAL_FILE);
        final boolean enabled = file != null && file.getName().endsWith(".fxs");
        actionEvent.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    protected String getExtension() {
        return FlexibleSearchFileType.getInstance().getDefaultExtension();
    }

    @Override
    protected String getConsoleName() {
        return "Hybris FS Console";
    }
}
