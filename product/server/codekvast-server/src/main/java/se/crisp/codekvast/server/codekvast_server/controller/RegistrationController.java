package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.server.agent_api.model.v1.Constraints;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.RegistrationService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Spring MVC Controller that handles registration.
 *
 * @author olle.hallin@crisp.se
 */
@Controller
@Slf4j
public class RegistrationController extends AbstractThymeleafController {

    public static final String IS_UNIQUE_PATH = "/register/isUnique";
    public static final String REGISTER_PATH = "/register";
    public static final int MAX_ORGANISATION_NAME_LENGTH = 100;

    private final RegistrationService registrationService;

    @NonNull
    private final Validator validator;

    @Inject
    public RegistrationController(RegistrationService registrationService, Validator validator) {
        this.registrationService = registrationService;
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
    private void onCodekvastException(CodekvastException e) {
        log.warn("Application exception: " + e);
    }

    @SuppressWarnings("SameReturnValue")
    @RequestMapping(value = REGISTER_PATH, method = RequestMethod.GET)
    public String registerGet(ModelMap modelMap) {
        modelMap.put("maxAppNameLength", Constraints.MAX_APP_NAME_LENGTH);
        modelMap.put("maxOrganisationNameLength", MAX_ORGANISATION_NAME_LENGTH);
        modelMap.put("maxEmailAddressLength", Constraints.MAX_EMAIL_ADDRESS_LENGTH);
        modelMap.put("maxFullNameLength", Constraints.MAX_FULL_NAME_LENGTH);
        modelMap.put("maxUsernameLength", Constraints.MAX_USER_NAME_LENGTH);
        return "register";
    }

    @RequestMapping(value = REGISTER_PATH, method = RequestMethod.POST)
    @ResponseBody
    public RegistrationResponse registerPost(@RequestBody @Valid RegistrationRequest data) throws CodekvastException {
        registrationService.registerUserAndOrganisation(data);
        return RegistrationResponse.builder().greeting(String.format("Welcome %s!", data.getFullName())).build();
    }

    @RequestMapping(value = IS_UNIQUE_PATH, method = RequestMethod.POST)
    @ResponseBody
    public IsNameUniqueResponse isUnique(@RequestBody @Valid IsNameUniqueRequest request) {
        return IsNameUniqueResponse.builder().isUnique(registrationService.isUnique(toKind(request.getKind()), request.getName())).build();
    }

    private RegistrationService.UniqueKind toKind(String kind) {
        switch (kind.toLowerCase()) {
        case "username":
            return RegistrationService.UniqueKind.USERNAME;
        case "organisationname":
            return RegistrationService.UniqueKind.ORGANISATION_NAME;
        case "emailaddress":
            return RegistrationService.UniqueKind.EMAIL_ADDRESS;
        default:
            throw new IllegalArgumentException("Unknown kind: " + kind);
        }
    }

    /**
     * Send by the registration wizard JavaScript to check whether a name is unique or not.
     *
     * @author olle.hallin@crisp.se
     */
    @Data
    @Setter(AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    public static class IsNameUniqueRequest {
        @NotBlank
        private String kind;

        @NotBlank
        private String name;
    }

    /**
     * Sent in response to a IsNameUniqueRequest back to the JavaScript layer in the registration wizard.
     *
     * @author olle.hallin@crisp.se
     */
    @Data
    @Setter(AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    public static class IsNameUniqueResponse {
        private boolean isUnique;
    }

    /**
     * Sent from JavaScript as the final step in the registration wizard.
     *
     * @author olle.hallin@crisp.se
     */
    @Data
    @Setter(AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    public static class RegistrationRequest {
        @NotBlank
        @Size(max = Constraints.MAX_FULL_NAME_LENGTH)
        private String fullName;

        @NotBlank
        @Size(max = Constraints.MAX_EMAIL_ADDRESS_LENGTH)
        @Email
        private String emailAddress;

        @NotBlank
        @Size(max = Constraints.MAX_USER_NAME_LENGTH)
        private String username;

        @NotBlank
        private String password;

        @NotBlank
        @Size(max = MAX_ORGANISATION_NAME_LENGTH)
        private String organisationName;
    }

    /**
     * Sent back to the JavaScript layer as successful response to a RegistrationRequest.
     *
     * @author olle.hallin@crisp.se
     */
    @Data
    @Setter(AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    public static class RegistrationResponse {
        private String greeting;
    }
}
