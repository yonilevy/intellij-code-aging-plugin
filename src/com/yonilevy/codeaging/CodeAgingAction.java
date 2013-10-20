package com.yonilevy.codeaging;

import com.google.common.collect.Maps;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.*;

public class CodeAgingAction extends AnAction {

    private final Logger log = Logger.getInstance(CodeAgingAction.class);
    private final Map<Document, List<RangeHighlighter>> highlighters = Maps.newHashMap();

    @SuppressWarnings("ConstantConditions")
    @Override public void actionPerformed(AnActionEvent event) {
        try {
            Editor editor = event.getData(PlatformDataKeys.EDITOR);
            Project project = event.getData(PlatformDataKeys.PROJECT);
            VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);

            if (editor == null) {
                return;
            }
            toggleHighlights(editor, editor.getDocument(), project, virtualFile);
        } catch (Exception e) {
            log.error("CodeAging Exception", e);
        }
    }

    private void toggleHighlights(Editor editor,
                                  Document document,
                                  Project project,
                                  VirtualFile virtualFile) throws VcsException {
        if (highlighters.containsKey(document)) {
            removeHighlights(editor, document);
            highlighters.remove(document);
        } else {
            highlighters.put(document, (new CodeAgingHighlighter(
                    project,
                    editor,
                    document,
                    virtualFile).highlight()));
        }
    }

    private void removeHighlights(Editor editor, Document document) {
        for (RangeHighlighter highlighter : highlighters.get(document)) {
            editor.getMarkupModel().removeHighlighter(highlighter);
        }
    }
}
