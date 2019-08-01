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

package com.intellij.idea.plugin.hybris.common.utils;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * Created 1:54 AM 08 June 2015
 *
 * @author Alexander Bartash <AlexanderBartash@gmail.com>
 */
public interface HybrisIcons {

    Icon IMPEX_FILE = IconLoader.getIcon("/icons/fileTypes/impexFile.svg");
    Icon FS_FILE = IconLoader.getIcon("/icons/fileTypes/flexibleSearchFile.svg");
    Icon BEAN_FILE = IconLoader.getIcon("/icons/beanIcon.svg");
    
    Icon HYBRIS_ICON = IconLoader.getIcon("/icons/hybrisIcon.svg");
    Icon HYBRIS_ICON_13x13 = IconLoader.getIcon("/icons/hybrisIcon_13x13.svg");
    Icon HYBRIS_REMOTE_ICON = IconLoader.getIcon("/icons/hybrisRemoteIcon.svg");
    
    Icon WAIT = IconLoader.getIcon("/icons/waitIcon.svg");
    Icon END = IconLoader.getIcon("/icons/endIcon.svg");
    Icon NOTIFY = IconLoader.getIcon("/icons/notifyIcon.svg");
    Icon ACTION = IconLoader.getIcon("/icons/actionIcon.svg");
    Icon SPLIT = IconLoader.getIcon("/icons/splitIcon.svg");
    Icon JOIN = IconLoader.getIcon("/icons/joinIcon.svg");
    Icon SCRIPT = IconLoader.getIcon("/icons/scriptIcon.svg");
    
    
    Icon TYPE_SYSTEM = IconLoader.getIcon("/icons/typeSystem.svg");
    Icon BEAN = IconLoader.getIcon("/icons/beanIcon.svg");
    Icon LOCALIZED = IconLoader.getIcon("/icons/localized.svg");

    Icon MACROS = IconLoader.getIcon("/icons/macros.png");
    
    
    interface Gutter {
        Icon POPULATOR = IconLoader.getIcon("/icons/gutter/populator.svg");
    }
    
    interface Console {
        Icon SOLR = IconLoader.getIcon("/icons/console/solr.svg");
    }
}
