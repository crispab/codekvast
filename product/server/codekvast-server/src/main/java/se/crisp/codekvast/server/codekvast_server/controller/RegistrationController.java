package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.server.agent.model.v1.Constraints;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.RegistrationRequest;
import se.crisp.codekvast.server.codekvast_server.model.RegistrationResponse;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;
import javax.validation.Valid;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Spring MVC Controller that handles registration.
 * <p/>
 *
 * @author Olle Hallin
 */
@Controller
@Slf4j
public class RegistrationController {

    private final UserService userService;

    @NonNull
    private final Validator validator;

    @Inject
    public RegistrationController(UserService userService, Validator validator) {
        this.userService = userService;
        this.validator = validator;
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
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    private void onApplicationException(CodekvastException e) {
        log.warn("Application exception: " + e);
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String registerGet(ModelMap modelMap) {
        modelMap.put("maxAppNameLength", Constraints.MAX_APP_NAME_LENGTH);
        modelMap.put("maxCustomerNameLength", Constraints.MAX_CUSTOMER_NAME_LENGTH);
        modelMap.put("maxEmailAddressLength", Constraints.MAX_EMAIL_ADDRESS_LENGTH);
        modelMap.put("maxFullNameLength", Constraints.MAX_FULL_NAME_LENGTH);
        modelMap.put("maxUsernameLength", Constraints.MAX_USER_NAME_LENGTH);
        return "register";
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @ResponseBody
    public RegistrationResponse registerPost(@RequestBody @Valid RegistrationRequest data) {
        return RegistrationResponse.builder().greeting(String.format("Welcome %s!", data.getFullName())).build();
    }

    @RequestMapping(value = "/register/isUnique", method = RequestMethod.GET)
    @ResponseBody
    public Boolean isUnique(@RequestParam("kind") String kind, @RequestParam("name") String name) {
        return userService.isUnique(toKind(kind), name);
    }

    private UserService.UniqueKind toKind(String kind) {
        switch (kind.toLowerCase()) {
        case "username":
            return UserService.UniqueKind.USERNAME;
        case "customername":
            return UserService.UniqueKind.CUSTOMER_NAME;
        default:
            throw new IllegalArgumentException("Unknown kind: " + kind);
        }
    }
}
