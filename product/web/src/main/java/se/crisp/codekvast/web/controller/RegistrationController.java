package se.crisp.codekvast.web.controller;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.web.model.RegistrationRequest;
import se.crisp.codekvast.web.model.RegistrationResponse;

import javax.validation.Valid;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Spring MVC Controller that handles REST requests from the Codekvast web UI.
 *
 * @author Olle Hallin
 */
@RestController
@Slf4j
public class RegistrationController {
    public static final String REGISTER_PATH = "/register";

    @NonNull
    private final Validator validator;

    @NonNull
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RegistrationController(Validator validator, JdbcTemplate jdbcTemplate) {
        this.validator = validator;
        this.jdbcTemplate = jdbcTemplate;
    }

    @InitBinder
    private void initBinder(WebDataBinder binder) {
        binder.setValidator(checkNotNull(validator, "validator is null"));
    }

    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.PRECONDITION_FAILED)
    private void onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation failure: " + e);
    }

    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.CONFLICT)
    private void onDuplicateKeyException(DuplicateKeyException e) {
        log.warn("Duplicate email address");
    }

    @RequestMapping(value = REGISTER_PATH, method = RequestMethod.POST)
    @ResponseBody
    public RegistrationResponse registerPost(@RequestBody @Valid RegistrationRequest data) {
        log.info("Received {}", data);
        saveRegistrationData(data);
        return RegistrationResponse.builder().greeting(String.format("Welcome %s!", data.getEmailAddress())).build();
    }

    private void saveRegistrationData(RegistrationRequest data) {
        jdbcTemplate.update("INSERT INTO PEOPLE(EMAIL_ADDRESS) VALUES(?)", data.getEmailAddress());
    }

}
