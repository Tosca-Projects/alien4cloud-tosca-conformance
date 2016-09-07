package org.alien4cloud.tosca.steps;

/**
 * Enumeration of the error codes from the TOSCA scenarios.
 */
public enum ToscaValidationErrorCode {
    InvalidTOSCAVersion,
    MissingTOSCAVersion,
    TOSCAVersionMustBeFirstLine,
    InvalidType,
    MissingArtifactType,
    UnknownDslDefinition,
    InvalidParentType,
    UnknownDataType,
    InvalidNativeTypeExtend,
    ValueTypeMismatch
}