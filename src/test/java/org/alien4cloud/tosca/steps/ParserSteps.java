package org.alien4cloud.tosca.steps;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.MapUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.validation.ToscaValidationGenerator;
import org.apache.commons.collections.MapUtils;
import org.junit.Assert;

import org.tosca.ToscaContextConfiguration;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.*;
import cucumber.api.PendingException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Cucumber steps implementation for parsing.
 */
@Slf4j
public class ParserSteps {
    // Contains all parsing errors that where not defined in the test case.
    public static List<ParsingError> UNDEFINED_PARSING_ERRORS;

    private ParsingResult<ArchiveRoot> parsingResult;

    /** Map the error code from the Tosca Validation scenarios to Alien4Cloud error codes. */
    private static Map<ToscaValidationErrorCode, ErrorCode> ERROR_CODE_MAP;
    /** Optional mapping of TOSCA error codes to a detailed problem message. */
    private static Map<ToscaValidationErrorCode, String> ERROR_PROBLEM_MAP;
    // Set of error codes that are considered as warning in alien and error in TOSCA.
    private static Set<ErrorCode> NON_STRICT_ERRORS;
    static {
        ERROR_CODE_MAP = Maps.newHashMap();
        ERROR_PROBLEM_MAP = Maps.newHashMap();
        NON_STRICT_ERRORS = Sets.newHashSet();

        ERROR_CODE_MAP.put(ToscaValidationErrorCode.InvalidTOSCAVersion, ErrorCode.UNKNOWN_TOSCA_VERSION);
        ERROR_CODE_MAP.put(ToscaValidationErrorCode.MissingTOSCAVersion, ErrorCode.MISSING_TOSCA_VERSION);
        ERROR_CODE_MAP.put(ToscaValidationErrorCode.TOSCAVersionMustBeFirstLine, ErrorCode.TOSCA_VERSION_NOT_FIRST);
        NON_STRICT_ERRORS.add(ErrorCode.TOSCA_VERSION_NOT_FIRST);
        ERROR_CODE_MAP.put(ToscaValidationErrorCode.MissingArtifactType, ErrorCode.TYPE_NOT_FOUND);
        ERROR_CODE_MAP.put(ToscaValidationErrorCode.InvalidType, ErrorCode.SYNTAX_ERROR);
        ERROR_CODE_MAP.put(ToscaValidationErrorCode.UnknownDslDefinition, ErrorCode.INVALID_YAML);
        ERROR_PROBLEM_MAP.put(ToscaValidationErrorCode.UnknownDslDefinition, "found undefined alias unknown_dsl_definition");
        ERROR_CODE_MAP.put(ToscaValidationErrorCode.InvalidParentType, ErrorCode.TYPE_NOT_FOUND);
        ERROR_CODE_MAP.put(ToscaValidationErrorCode.UnknownDataType, ErrorCode.TYPE_NOT_FOUND);

    }

    @Given("^I parse the archive at \"(.*?)\"$")
    public void i_parse_the_archive_at(String location) throws Throwable {
        Path archivePath = ToscaValidationGenerator.SCENARIO_DIRECTORY.resolve(location);

        ToscaParser toscaParser = ToscaContextConfiguration.getParser();
        try {
            parsingResult = toscaParser.parseFile(archivePath);
        } catch (ParsingException e) {
            parsingResult = new ParsingResult<>();
            parsingResult.setContext(new ParsingContext());
            parsingResult.getContext().setFileName(e.getFileName());
            parsingResult.getContext().setParsingErrors(e.getParsingErrors());
        } finally {
            if (parsingResult != null && parsingResult.getContext() != null && parsingResult.getContext().getParsingErrors() != null) {
                UNDEFINED_PARSING_ERRORS = Lists.newArrayList(parsingResult.getContext().getParsingErrors());
            }
        }
    }

    @Then("^the parsing is a success$")
    public void the_parsing_is_a_success() throws Throwable {
        displayErrors();
        Assert.assertFalse(parsingResult.hasError(ParsingErrorLevel.ERROR));
    }

    public void displayErrors() {
        displayErrors(ParsingErrorLevel.ERROR);
        displayErrors(ParsingErrorLevel.WARNING);
        displayErrors(ParsingErrorLevel.INFO);
    }

    private void displayErrors(ParsingErrorLevel level) {
        for (int i = 0; i < parsingResult.getContext().getParsingErrors().size(); i++) {
            ParsingError error = parsingResult.getContext().getParsingErrors().get(i);
            if (error.getErrorLevel().equals(level)) {
                log.info("{}: parsing error in {}:\n", parsingResult.getContext().getFileName(), error);
            }
        }
    }

    @Then("^the parsing failed$")
    public void the_parsing_failed() throws Throwable {
        // map tolerance
        mapTolerance();
        Assert.assertTrue(parsingResult.hasError(ParsingErrorLevel.ERROR));
    }

    // ErrorCode errorCode, String context, Mark startMark, String problem, Mark endMark, String note
    private void mapTolerance() {
        List<ParsingError> toleratedErrors = parsingResult.getContext().getParsingErrors().stream()
                .filter(parsingError -> NON_STRICT_ERRORS.contains(parsingError.getErrorCode()))
                .map(parsingError -> new ParsingError(ParsingErrorLevel.ERROR, parsingError)).collect(Collectors.toList());
        parsingResult.getContext().getParsingErrors().addAll(toleratedErrors);
    }

    @Then("^there is an error with code \"(.*?)\" at line (\\d+)$")
    public void there_is_an_error_with_code_at_line(String codeAsString, int line) throws Throwable {
        try {
            ToscaValidationErrorCode toscaErrorCode = ToscaValidationErrorCode.valueOf(codeAsString);
            ErrorCode alienErrorCode = ERROR_CODE_MAP.get(toscaErrorCode);
            assertNotNull("ErrorCode not mapped to alien error code", alienErrorCode);
            String errorProblem = ERROR_PROBLEM_MAP.get(toscaErrorCode);

            ParsingError parsingError = getParsingError(alienErrorCode);
            assertNotNull("No error found by alien4cloud parser while expecting <" + alienErrorCode + ">", parsingError);
            if (errorProblem != null) {
                assertEquals(errorProblem, parsingError.getProblem());
            }
            assertEquals(line, parsingError.getStartMark().getLine());
        } catch (IllegalArgumentException e) {
            // unknown error code, test is not mapped yet
            throw new PendingException();
        }
    }

    private ParsingError getParsingError(ErrorCode expectedCode) {
        ParsingError error = getParsingError(ParsingErrorLevel.ERROR, expectedCode);
        if (error == null) {
            error = getParsingError(ParsingErrorLevel.WARNING, expectedCode);
        }
        return error;
    }

    private ParsingError getParsingError(ParsingErrorLevel level, ErrorCode expectedCode) {
        for (int i = 0; i < parsingResult.getContext().getParsingErrors().size(); i++) {
            ParsingError error = parsingResult.getContext().getParsingErrors().get(i);
            if (error.getErrorLevel().equals(level) && expectedCode.equals(error.getErrorCode())) {
                return error;
            }
        }
        return null;
    }
}