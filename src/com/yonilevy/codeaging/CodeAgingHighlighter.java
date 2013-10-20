package com.yonilevy.codeaging;

import com.google.common.base.Function;
import com.google.common.collect.*;
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

public class CodeAgingHighlighter {

    private static final float BRIGHTNESS_MIN = 0.2f;
    private static final float BRIGHTNESS_MAX = 0.6f;
    private static final float HUE = 0.145f;
    private static final float SATURATION = 0.188f;

    private final Project project;
    private final Editor editor;
    private final Document document;
    private final VirtualFile virtualFile;

    public CodeAgingHighlighter(Project project, Editor editor, Document document, VirtualFile virtualFile) {
        this.project = project;
        this.editor = editor;
        this.document = document;
        this.virtualFile = virtualFile;
    }

    public List<RangeHighlighter> highlight() throws VcsException {
        int numLines = document.getLineCount();
        List<Float> normalizedAges = calcNormalizedLineGeneration(numLines);

        List<RangeHighlighter> highlighters = Lists.newArrayList();
        for (int currentLine = 0 ; currentLine < numLines ; ++currentLine) {
            highlighters.add(highlightLine(currentLine, normalizedAges.get(currentLine)));
        }

        return highlighters;
    }

    private List<Float> calcNormalizedLineGeneration(int numLines) throws VcsException {
        return normalizeLineGenerations(getLineDates(numLines));
    }

    private RangeHighlighter highlightLine(int currentLine,
                                           Float brightnessCoefficient) {
        float brightness = (brightnessCoefficient * (BRIGHTNESS_MAX - BRIGHTNESS_MIN)) + BRIGHTNESS_MIN;
        TextAttributes attr = new TextAttributes();
        attr.setBackgroundColor(JBColor.getHSBColor(HUE, SATURATION, brightness));
        return editor.getMarkupModel().addLineHighlighter(currentLine, HighlighterLayer.ADDITIONAL_SYNTAX, attr);
    }

    private List<Float> normalizeLineGenerations(List<Date> lineDates) {
        List<Date> sortedLineDates = Lists.newArrayList(Sets.newHashSet(lineDates));
        Collections.sort(sortedLineDates);
        final Map<Date, Float> dateToDateNormalized = Maps.newHashMap();
        for (int i = 0 ; i < sortedLineDates.size() ; ++i) {
            int divisor = sortedLineDates.size() - 1;
            dateToDateNormalized.put(sortedLineDates.get(i), (float) i / (divisor == 0 ? 1 : divisor));
        }
        return Lists.newArrayList(Collections2.transform(lineDates, new Function<Date, Float>() {
            public Float apply(Date date) {
                return dateToDateNormalized.get(date);
            }
        }));
    }

    @SuppressWarnings("ConstantConditions")
    private List<Date> getLineDates(int numLines) throws VcsException {
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
}
