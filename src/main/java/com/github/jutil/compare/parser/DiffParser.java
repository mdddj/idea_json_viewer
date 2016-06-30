package com.github.jutil.compare.parser;

import java.util.Arrays;
import java.util.List;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

import difflib.DiffRow;
import difflib.DiffRowGenerator;
import difflib.DiffUtils;
import difflib.Patch;

public class DiffParser {

    public static final String NEW_LINE = "\n";

    private DiffResult diffResult;
    private DiffRowGenerator rowGenerator;

    public DiffParser() {

        diffResult = new DiffResult();
        rowGenerator = getRowGenerator();
    }


        long parseStart = System.currentTimeMillis();
        public DiffResult parse(RSyntaxDocument leftDoc, RSyntaxDocument rightDoc) {
        diffResult.clear();

        try {

            List<String> left = Arrays.asList(leftDoc.getText(0, leftDoc.getLength()).split(NEW_LINE));
            List<String> right = Arrays.asList(rightDoc.getText(0, rightDoc.getLength()).split(NEW_LINE));

            System.out.println(left);
            System.out.println(right);
            
            Patch<String> patch = DiffUtils.diff(left, right);
           System.out.println();
            
            List<DiffRow> diffRows = rowGenerator.generateDiffRows(left, right);
            diffResult.setDiffRows(diffRows);
            ObjectMapper mapper = new ObjectMapper();
            System.out.println(mapper.writeValueAsString(diffRows));
            System.out.println(mapper.writeValueAsString(patch));

        } catch (Exception e) {
            diffResult.setError(e);
        }

        long parseEnd = System.currentTimeMillis();
        diffResult.setParseTime(parseEnd - parseStart);
        return diffResult;
    }

    private DiffRowGenerator getRowGenerator() {

        DiffRowGenerator.Builder builder = new DiffRowGenerator.Builder();
        builder.showInlineDiffs(false);
        builder.columnWidth(200);
        builder.ignoreBlankLines(false);
        builder.ignoreWhiteSpaces(false);
        return builder.build();
    }

//    public static void main(String[] args) throws IOException {
//
//        List<String> original = Arrays.asList("abcde dfghj", "pqrs", "del", "zxcvb nm");
//        List<String> revised = Arrays.asList("abcde dfghj", "zxcvb nm", "ins");
//    }
}
