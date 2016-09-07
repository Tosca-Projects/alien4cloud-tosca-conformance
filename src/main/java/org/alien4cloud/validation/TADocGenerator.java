package org.alien4cloud.validation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.docx4j.Docx4J;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;
import org.docx4j.wml.P;

/**
 * Generates a word doc out of test assertions.
 */
@Slf4j
public class TADocGenerator {

    public static void main(String... args) throws Docx4JException, IOException {
        ToscaValidationGenerator generator = new ToscaValidationGenerator();
        Map<String, List<TestAssertion>> assertionMap = generator.checkoutAndIterate();

        WordprocessingMLPackage wordMLPackage = Docx4J.load(new java.io.File("/Users/lucboutier/Desktop/TOSCA-Interop-Template.docx"));
        MainDocumentPart mdp = wordMLPackage.getMainDocumentPart();
        log.info("Word document opened");

        Map<String, Object> leveledMap = Maps.newTreeMap();

        for (Map.Entry<String, List<TestAssertion>> entry : assertionMap.entrySet()) {
            String id = entry.getKey().split("-")[0];
            String[] levels = id.split("\\.");
            Map<String, Object> current = leveledMap;
            for (String level : levels) {
                if (!current.containsKey(level)) {
                    current.put(level, Maps.newTreeMap());
                }
                current = (Map<String, Object>) current.get(level);
            }
            current.put(entry.getKey(), entry.getValue());
        }

        generateContent(mdp, 0, "", leveledMap);

        log.info("Saving");
        Docx4J.save(wordMLPackage, new java.io.File("target/ta.docx"), Docx4J.FLAG_SAVE_ZIP_FILE);
        log.info("Saved");
    }

    private static void generateContent(MainDocumentPart mdp, int level, String parentLevel, Map<String, Object> leveledContentMap) {
        if (level > 1) {
            mdp.addStyledParagraphOfText(getStyleFromLevel(level), parentLevel);
        }
        for (Map.Entry<String, Object> leveledContentEntry : leveledContentMap.entrySet()) {
            List<Map<String, Object>> children = Lists.newArrayList();
            if (Map.class.isAssignableFrom(leveledContentEntry.getValue().getClass())) {
                children.add((Map<String, Object>) leveledContentEntry.getValue());
            } else {
                // generate test assertion
                List<TestAssertion> levelTA = (List<TestAssertion>) leveledContentEntry.getValue();
                for (TestAssertion testAssertion : levelTA) {
                    generateTestAssertion(mdp, testAssertion);
                }
            }
            for (Map<String, Object> child : children) {
                String contentLevel = parentLevel == null ? leveledContentEntry.getKey() : parentLevel + "." + leveledContentEntry.getKey();
                generateContent(mdp, level + 1, contentLevel, child);
            }
        }
    }

    private static void generateTestAssertion(MainDocumentPart mdp, TestAssertion testAssertion) {
        mdp.addParagraphOfText(testAssertion.getId());
    }

    private static String getStyleFromLevel(int level) {
        if (level > 0) {
            return "Heading" + level;
        }
        return "";
    }
}