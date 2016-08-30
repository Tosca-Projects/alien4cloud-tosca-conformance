package org.alien4cloud.tosca.report;

import java.util.Set;

import com.google.common.collect.Sets;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Report for a feature.
 */
@Getter
@Setter
@EqualsAndHashCode(of = "name")
public class FeatureReport {
    private String name;
    private String status;
    private Set<ScenarioReport> scenarios = Sets.newHashSet();
}