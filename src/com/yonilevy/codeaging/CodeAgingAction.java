package com.yonilevy.codeaging;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.impl.UpToDateLineNumberProviderImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.vcsUtil.VcsUtil;

import java.util.*;

public class CodeAgingAction extends AnAction {

    private static final float BRIGHTNESS_MIN = 0.2f;
    private static final float BRIGHTNESS_MAX = 0.6f;
    private static final float HUE = 0.145f;
    private static final float SATURATION = 0.188f;

    private final Logger log = Logger.getInstance(CodeAgingAction.class);
    private final Map<Document, List<RangeHighlighter>> highlighters = Maps.newHashMap();

    @SuppressWarnings("ConstantConditions")
    @Override public void actionPerformed(AnActionEvent event) {
        try {
            Editor editor = event.getData(PlatformDataKeys.EDITOR);
            Project project = event.getData(PlatformDataKeys.PROJECT);
            VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);

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
            highlighters.put(document,
                    addHighlights(project, editor, document, virtualFile));
        }
    }

    private void removeHighlights(Editor editor, Document document) {
        for (RangeHighlighter highlighter : highlighters.get(document)) {
            editor.getMarkupModel().removeHighlighter(highlighter);
        }
    }

    private List<RangeHighlighter> addHighlights(Project project,
                                                 Editor editor,
                                                 Document document,
                                                 VirtualFile virtualFile) throws VcsException {
        int numLines = document.getLineCount();
        List<Float> normalizedAges = calcNormalizedLineAges(project, document, virtualFile, numLines);

        List<RangeHighlighter> highlighters = Lists.newArrayList();
        for (int currentLine = 0 ; currentLine < numLines ; ++currentLine) {
            highlighters.add(highlightLine(editor, currentLine, normalizedAges.get(currentLine)));
        }

        return highlighters;
    }

    private List<Float> calcNormalizedLineAges(Project project,
                                               Document document,
                                               VirtualFile virtualFile,
                                               int numLines) throws VcsException {
        return normalizeLineAges(calcLineAges(getLineDates(project, document, virtualFile, numLines)));
    }

    private RangeHighlighter highlightLine(Editor editor, int currentLine, Float brightnessCoefficient) {
        float brightness = (brightnessCoefficient * (BRIGHTNESS_MAX - BRIGHTNESS_MIN)) + BRIGHTNESS_MIN;
        TextAttributes attr = new TextAttributes();
        attr.setBackgroundColor(JBColor.getHSBColor(HUE, SATURATION, brightness));
        return editor.getMarkupModel().addLineHighlighter(currentLine, HighlighterLayer.ADDITIONAL_SYNTAX, attr);
    }

    private List<Float> normalizeLineAges(List<Long> lineAges) {
        final Long maxAge = Collections.max(lineAges);
        return Lists.newArrayList(Collections2.transform(lineAges, new Function<Long, Float>() {
            public Float apply(Long age) {
                return maxAge == 0 ? 1f : (float) age / maxAge;
            }
        }));
    }

    @SuppressWarnings("ConstantConditions")
    private List<Date> getLineDates(final Project project,
                                    Document document,
                                    final VirtualFile virtualFile,
                                    int numLines) throws VcsException {
        final Date now = new Date();
        final FileAnnotation annotation = VcsUtil.getVcsFor(project, virtualFile).
                getAnnotationProvider().annotate(virtualFile);
        final UpToDateLineNumberProviderImpl lineNumConverter = new UpToDateLineNumberProviderImpl(document, project);

        return Lists.newArrayList(Collections2.transform(Ranges.closedOpen(0, numLines).
                asSet(DiscreteDomains.integers()), new Function<Integer, Date>() {
            public Date apply(Integer lineNum) {
                Integer headLineNum = lineNumConverter.getLineNumber(lineNum);

                // There's an uncommitted line, set its date as now.
                if (headLineNum == -1L) return now;

                Date lineDate = annotation.getLineDate(headLineNum);
                return lineDate == null ? now : lineDate;
            }
        }));
    }

    @SuppressWarnings("ConstantConditions")
    private List<Long> calcLineAges(List<Date> lineDates) {
        final Date maxLineDate = Collections.max(lineDates);
        return Lists.newArrayList(Collections2.transform(lineDates, new Function<Date, Long>() {
            public Long apply(Date lineDate) {
                return maxLineDate.getTime() - lineDate.getTime();
            }
        }));
    }
}
