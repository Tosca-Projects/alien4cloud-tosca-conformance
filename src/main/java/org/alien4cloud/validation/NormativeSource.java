package org.alien4cloud.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NormativeSource {
    private String documentId;
    private String versionId;
    private String section;
}
