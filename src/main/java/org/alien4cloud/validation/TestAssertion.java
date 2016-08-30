package org.alien4cloud.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Represents a scenario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestAssertion {
    private String file;
    private String id;
    private String description;
    private String target;
    private String predicate;
    private String prescription_level;
    private String conformanceTarget;
    private NormativeSource normativeSource;
    private List<TestError> errors;

    public boolean hasErrors() {
        return errors != null && errors.size() > 0;
    }
}
