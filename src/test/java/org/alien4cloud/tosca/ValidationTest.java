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
        // "target/features/3.1.2-tosca_definitions_version.feature"
        // "target/features/3.5.1-description.feature"
        // "target/features/3.5.5-repositories.feature"
        // "target/features/3.6.3-artifact_type.feature"
        // "target/features/3.6.5-data_type.feature"
        // "target/features/3.9.1.1-metadata.feature"
        // "target/features/3.9.3.3-metadata.feature"
        // "target/features/3.9.3.4-metadata.feature"
        // "target/features/3.9.3.5-metadata.feature"
        // "target/features/3.9.3.7-dsl_definitions.feature"
        //
}, plugin = "org.alien4cloud.tosca.report.ToscaReporter", format = { "pretty", "html:target/cucumber/properties",
        "json:target/cucumber/cucumber-properties.json" })
public class ValidationTest {
}