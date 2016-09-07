package org.alien4cloud.validation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.IgnoreYaml;
import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.git.RepositoryManager;
import alien4cloud.tosca.serializer.VelocityUtil;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.MapUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Tosca Validation Generator is used to generate cucumber features from TOSCA validation test suite.
 */
@Slf4j
public class ToscaValidationGenerator {
    private final static Path TOSCA_LOCAL_REPO_DIR = Paths.get("target/repository/");
    private final static Path GIT_DIR = Paths.get("target/git/");
    private final static Path FEATURES_DIR = Paths.get("target/features");
    public static Path SCENARIO_DIRECTORY = GIT_DIR.resolve("tosca-test-assertions").resolve("Parser-Validator");

    private final RepositoryManager repositoryManager = new RepositoryManager();

    /**
     * Program main.
     * 
     * @param args Arguments
     */
    public static void main(String[] args) throws IOException {
        ToscaValidationGenerator generator = new ToscaValidationGenerator();
        generator.checkoutAndIterate();
    }

    public Map<String, List<TestAssertion>> checkoutAndIterate() throws IOException {
        // use git to clone repository
        repositoryManager.cloneOrCheckout(GIT_DIR, "https://github.com/lucboutier/tosca-test-assertions.git", "master", "tosca-test-assertions");
        repositoryManager.cloneOrCheckout(GIT_DIR, "https://github.com/alien4cloud/alien4cloud.github.io.git", "sources", "alien4cloud.github.io");

        // Prepare the local TOSCA repository to include the normative types
        Files.createDirectories(TOSCA_LOCAL_REPO_DIR.resolve("tosca-normative-types").resolve("1.0.0"));
        FileUtil.zip(GIT_DIR.resolve("tosca-test-assertions").resolve("Normative-types").resolve("normative-types.yml"),
                TOSCA_LOCAL_REPO_DIR.resolve("tosca-normative-types").resolve("1.0.0").resolve("tosca-normative-types-1.0.0.csar"));

        // iterate over scenarios
        FileUtil.delete(FEATURES_DIR);
        Files.createDirectories(FEATURES_DIR); // ensure directory is created

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(SCENARIO_DIRECTORY)) {
            Iterator<Path> iter = stream.iterator();
            Map<String, List<TestAssertion>> assertionMap = Maps.newLinkedHashMap();
            while (iter.hasNext()) {
                Path path = iter.next();
                if (!path.getFileName().toString().contains("FIXME")) {
                    TestAssertion assertion = generateScenario(path);
                    if (assertion != null) {
                        String[] assertionIdParts = assertion.getId().split("-");
                        MapUtil.addToList(assertionMap, assertionIdParts[0] + "-" + assertionIdParts[1], assertion);
                    }
                }
            }
            // let's generate features and scenarios
            for (Map.Entry<String, List<TestAssertion>> featureEntry : assertionMap.entrySet()) {
                Path featurePath = FEATURES_DIR.resolve(featureEntry.getKey().concat(".feature"));

                Map<String, Object> velocityCtx = new HashMap<>();
                velocityCtx.put("name", featureEntry.getKey());
                velocityCtx.put("scenarios", featureEntry.getValue());
                VelocityUtil.generate("feature.vm", new FileWriter(featurePath.toFile()), velocityCtx);
            }
            return assertionMap;
        }
    }

    private TestAssertion generateScenario(Path scenarioPath) throws FileNotFoundException {
        if (scenarioPath.toFile().isDirectory()) {
            log.error("Directory generation is not currently supported, skipping {}", scenarioPath.getFileName());
            return null;
        } else {
            Yaml yaml = new IgnoreYaml();

            Map<String, Object> map = (Map<String, Object>) yaml.load(new FileInputStream(scenarioPath.toFile()));
            Map<String, String> metadata = (Map<String, String>) map.get("metadata");
            String target = metadata.get("oasis.testAssertion.target").trim();
            if (target.endsWith(".")) {
                target = target.substring(0, target.length() - 1);
            }
            target = target.replaceAll("\\(", "[");
            target = target.replaceAll("\\)", "]");
            target = target.replaceAll("\\.", ",");
            TestAssertion assertion = new TestAssertion(SCENARIO_DIRECTORY.relativize(scenarioPath).toString(), metadata.get("oasis.testAssertion.id"),
                    metadata.get("oasis.testAssertion.description"), target, metadata.get("oasis.testAssertion.predicate"),
                    metadata.get("oasis.testAssertion.prescription_level"), metadata.get("oasis.testAssertion.tags.conformancetarget"),
                    new NormativeSource(metadata.get("oasis.testAssertion.normativeSource.refSourceItem.documentId"),
                            metadata.get("oasis.testAssertion.normativeSource.refSourceItem.versionId"),
                            metadata.get("oasis.testAssertion.normativeSource.textSourceItem.section")),
                    Lists.<TestError> newArrayList());
            String errorCodes = metadata.get("oasis.testAssertion.tags.errors");
            if (errorCodes != null) {
                String errorLines = String.valueOf(metadata.get("oasis.testAssertion.tags.errors_lines"));
                String[] errorCodeArray = errorCodes.split(",");
                String[] errorLineArray = errorLines.split(",");
                for (int i = 0; i < errorCodeArray.length; i++) {
                    TestError error = new TestError(errorCodeArray[i].trim(), Integer.valueOf(errorLineArray[i].trim()).intValue());
                    assertion.getErrors().add(error);
                }
            }
            return assertion;
        }
    }
}
