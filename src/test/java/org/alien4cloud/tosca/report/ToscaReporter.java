package org.alien4cloud.tosca.report;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import alien4cloud.tosca.serializer.VelocityUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.alien4cloud.tosca.steps.ParserSteps;

import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.*;

/**
 * Create alien4cloud tosca support report.
 */
public class ToscaReporter implements Reporter, Formatter {
    private static final List<String> STATUS = Lists.newArrayList("passed", "passed with limitation(s)", "skipped", "pending", "undefined", "failed");
    // feature/ scenario / report
    private Set<FeatureReport> features = Sets.newLinkedHashSet();
    private FeatureReport currentFeature;
    private ScenarioReport currentScenario;
    private Scenario scenario;

    @Override
    public void feature(Feature feature) {
        currentFeature = new FeatureReport();
        currentFeature.setName(feature.getName());
        currentFeature.setStatus(STATUS.get(0));
        features.add(currentFeature);
    }

    @Override
    public void scenario(Scenario scenario) {
        currentScenario = new ScenarioReport();
        currentScenario.setName(scenario.getName().replaceAll("\\[", "(").replaceAll("\\]", ")"));
        currentScenario.setStatus(STATUS.get(0));
        currentFeature.getScenarios().add(currentScenario);
    }

    @Override
    public void result(Result result) {
        int statusIndex = STATUS.indexOf(result.getStatus());
        if (statusIndex == 0 && hasError(ParsingErrorLevel.WARNING, ParserSteps.UNDEFINED_PARSING_ERRORS)) {
            statusIndex = 1;
        }

        int scenarioStatusIndex = STATUS.indexOf(currentScenario.getStatus());
        if (scenarioStatusIndex < statusIndex) {
            currentScenario.setStatus(STATUS.get(statusIndex));
        }

        int featureStatusIndex = STATUS.indexOf(currentFeature.getStatus());
        if (featureStatusIndex < statusIndex) {
            currentFeature.setStatus(STATUS.get(statusIndex));
        }

        currentScenario.setWarningErrors(ofLevel(ParsingErrorLevel.WARNING, ParserSteps.UNDEFINED_PARSING_ERRORS));
        currentScenario.setInfoErrors(ofLevel(ParsingErrorLevel.INFO, ParserSteps.UNDEFINED_PARSING_ERRORS));
    }

    private List<ParsingError> ofLevel(ParsingErrorLevel level, List<ParsingError> errors) {
        return errors.stream().filter(parsingError -> parsingError.getErrorLevel().equals(level)).collect(Collectors.toList());
    }

    private boolean hasError(ParsingErrorLevel level, List<ParsingError> errors) {
        for (ParsingError error : errors) {
            if (level == null || level.equals(error.getErrorLevel())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void close() {
        try {
            Map<String, Object> velocityCtx = new HashMap<>();
            velocityCtx.put("features", features);
            VelocityUtil.generate("report.vm", new FileWriter(Paths.get("target/git/alien4cloud.github.io/documentation/1.3.0/devops_guide/toscareport.md").toFile()), velocityCtx);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void before(Match match, Result result) {
    }

    @Override
    public void after(Match match, Result result) {
    }

    @Override
    public void match(Match match) {

    }

    @Override
    public void embedding(String s, byte[] bytes) {
    }

    @Override
    public void write(String s) {
    }

    @Override
    public void syntaxError(String s, String s1, List<String> list, String s2, Integer integer) {

    }

    @Override
    public void uri(String s) {

    }

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
    }

    @Override
    public void examples(Examples examples) {
    }

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {
    }

    @Override
    public void background(Background background) {
    }

    @Override
    public void step(Step step) {
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
    }

    @Override
    public void done() {
    }

    @Override
    public void eof() {
    }
}
