package org.alien4cloud.tosca.report;

import alien4cloud.tosca.parser.ParsingError;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Report for a scenario.
 */
@Getter
@Setter
@EqualsAndHashCode(of = "name")
public class ScenarioReport {
    private String name;
    private String status;
    // contains all parsing errors but the ones specified by the test case.
    private List<ParsingError> warningErrors;
    private List<ParsingError> infoErrors;
}