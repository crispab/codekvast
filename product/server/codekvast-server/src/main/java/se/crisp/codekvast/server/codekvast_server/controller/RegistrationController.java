package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import se.crisp.codekvast.server.agent.model.v1.Constraints;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;

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

    @Inject
    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String register(ModelMap modelMap) {
        modelMap.put("maxAppNameLength", Constraints.MAX_APP_NAME_LENGTH);
        modelMap.put("maxCustomerNameLength", Constraints.MAX_CUSTOMER_NAME_LENGTH);
        modelMap.put("maxEmailAddressLength", Constraints.MAX_EMAIL_ADDRESS_LENGTH);
        modelMap.put("maxFullNameLength", Constraints.MAX_FULL_NAME_LENGTH);
        modelMap.put("maxUsernameLength", Constraints.MAX_USER_NAME_LENGTH);
        return "register";
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
