package org.alien4cloud.tosca;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

/**
 * Cucumber validation entry point for TOSCA ParserSteps validation.
 */
@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "target/features/"
        // "classpath:features/"
        //
}, plugin = "org.alien4cloud.tosca.report.ToscaReporter", format = { "pretty", "html:target/cucumber/properties", "json:target/cucumber/cucumber-properties.json" })
public class ValidationTest {
}