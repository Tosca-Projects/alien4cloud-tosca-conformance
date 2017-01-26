package org.alien4cloud.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.docx4j.Docx4J;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;

import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

/**
 * Generates a word doc out of test assertions.
 */
@Slf4j
public class TADocGenerator {
    private static final ObjectFactory FACTORY = Context.getWmlObjectFactory();

    public static class Node {
        private String id;
        private String title;
        private Node parent;
        private List<Node> children;
        private List<TestAssertion> testAssertions;
    }

    public static void main(String... args) throws Docx4JException, IOException {
        ToscaValidationGenerator generator = new ToscaValidationGenerator();
        Map<String, List<TestAssertion>> assertionMap = generator.checkoutAndIterate();

        WordprocessingMLPackage specificationDoc = WordprocessingMLPackage
                .load(new java.io.File("/Users/lucboutier/Desktop/TOSCA/TOSCA-Simple-Profile-YAML-v1.0-csprd02.docx"));
        MainDocumentPart specMDP = specificationDoc.getMainDocumentPart();
        List<Object> specContent = specMDP.getContent();

        Node parent = new Node();
        parent.title = "Root";
        parent.children = new ArrayList<>();
        parent.id = null;
        Node root = parent;
        int currentLevel = 1;

        Map<String, Node> nodeByIds = new HashMap<>();

        for (Object ob : specContent) {
            if (ob instanceof P) {
                P paragraph = (P) ob;
                if (paragraph.getPPr() != null && paragraph.getPPr().getPStyle() != null) {
                    String style = paragraph.getPPr().getPStyle().getVal();
                    if (style.startsWith("Heading")) {
                        try {
                            int level = Integer.valueOf(style.substring(7));

                            if (level > currentLevel) {
                                currentLevel++;
                                parent = parent.children.get(parent.children.size() - 1);
                            }
                            while (level < currentLevel) {
                                parent = parent.parent;
                                currentLevel--;
                            }
                            Node node = new Node();
                            node.title = paragraph.toString();
                            node.id = parent.id == null ? String.valueOf(parent.children.size() + 1) : parent.id + "." + (parent.children.size() + 1);
                            node.parent = parent;
                            node.children = new ArrayList<>();
                            node.testAssertions = new ArrayList<>();
                            parent.children.add(node);
                            nodeByIds.put(node.id, node);
                        } catch (Exception e) {
                            // not a classical heading style; skip
                        }
                    }
                }

            }
        }

        WordprocessingMLPackage wordMLPackage = Docx4J.load(new java.io.File("/Users/lucboutier/Desktop/TOSCA-Interop-Template.docx"));
        MainDocumentPart mdp = wordMLPackage.getMainDocumentPart();
        log.info("Word document opened");

        Map<String, Object> leveledMap = Maps.newTreeMap();

        for (Map.Entry<String, List<TestAssertion>> entry : assertionMap.entrySet()) {
            String id = entry.getKey().split("-")[0];

            Node node = nodeByIds.get(id);
            if (node == null) {
                System.out.println("Node not found for id " + id);
            } else {
                node.testAssertions = entry.getValue();
            }
        }

        // The template document starts at section 3 to match specification grammar definition section.
        generateTestAssertionDoc(mdp, 1, root.children.get(2));

        log.info("Saving");
        Docx4J.save(wordMLPackage, new java.io.File("target/tosca-test-assertions.docx"), Docx4J.FLAG_SAVE_ZIP_FILE);
        log.info("Saved");
    }

    private static void generateTestAssertionDoc(MainDocumentPart mdp, int level, Node node) {
        if (1 < level && level < 4) {
            mdp.addStyledParagraphOfText(getStyleFromLevel(level), node.title);
        }

        // generate test assertions
        for (TestAssertion testAssertion : node.testAssertions) {
            generateTestAssertion(mdp, testAssertion);
        }

        for (Node child : node.children) {
            generateTestAssertionDoc(mdp, level + 1, child);
        }
    }

    private static void generateTestAssertion(MainDocumentPart mdp, TestAssertion testAssertion) {
        addTAElement(mdp, "Id: ", testAssertion.getId());
        addTAElement(mdp, "Prerequisite: ", testAssertion.getPrerequisite());
        addTAElement(mdp, "Description: ", testAssertion.getDescription());
        addTAElement(mdp, "Target: ", testAssertion.getTarget());
        addTAElement(mdp, "Predicate: ", testAssertion.getPredicate());
        addTAElement(mdp, "Prescription level: ", testAssertion.getPrescription_level());
        addTAElement(mdp, "Conformance target: ", testAssertion.getConformanceTarget());
        mdp.addParagraphOfText("");
    }

    private static void addTAElement(MainDocumentPart mdp, String title, String value) {
        P p = FACTORY.createP();

        Text text = FACTORY.createText();
        text.setValue(title);
        text.setSpace("preserve");
        R run = FACTORY.createR();
        run.getContent().add(text);
        run.setRPr(FACTORY.createRPr());
        run.getRPr().setB(new BooleanDefaultTrue());
        p.getContent().add(run);

        text = FACTORY.createText();
        text.setSpace("preserve");
        text.setValue(value);
        run = FACTORY.createR();
        run.getContent().add(text);
        run.setRPr(FACTORY.createRPr());
        p.getContent().add(run);

        mdp.addObject(p);
    }

    private static String getStyleFromLevel(int level) {
        if (level > 0) {
            return "Heading" + level;
        }
        return "";
    }
}