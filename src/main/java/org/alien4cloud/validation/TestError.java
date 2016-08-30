package org.alien4cloud.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an error in a test assertion.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TestError {
    private String code;
    private int line;
}
