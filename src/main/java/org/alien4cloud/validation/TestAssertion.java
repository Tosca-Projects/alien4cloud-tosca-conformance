package org.alien4cloud.validation;

import java.util.List;

import lombok.*;

/**
 * Represents a scenario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TestAssertion implements Comparable<TestAssertion> {
    private String file;
    private String id;
    private String description;
    private String prerequisite;
    private String target;
    private String predicate;
    private String prescription_level;
    private String conformanceTarget;
    private NormativeSource normativeSource;
    private List<TestError> errors;

    public boolean hasErrors() {
        return errors != null && errors.size() > 0;
    }

    @Override
    public int compareTo(TestAssertion o) {
        return id.compareTo(o.getId());
    }
}
