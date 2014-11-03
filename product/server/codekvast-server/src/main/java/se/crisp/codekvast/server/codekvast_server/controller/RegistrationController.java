package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import se.crisp.codekvast.server.agent.model.v1.Constraints;

/**
 * A Spring MVC Controller that handles registration.
 * <p/>
 *
 * @author Olle Hallin
 */
@Controller
@Slf4j
public class RegistrationController {

    @RequestMapping(value = {"/register"}, method = RequestMethod.GET)
    public String register(ModelMap modelMap) {
        modelMap.put("maxAppNameLength", Constraints.MAX_APP_NAME_LENGTH);
        modelMap.put("maxCustomerNameLength", Constraints.MAX_CUSTOMER_NAME_LENGTH);
        modelMap.put("maxEmailAddressLength", Constraints.MAX_EMAIL_ADDRESS_LENGTH);
        modelMap.put("maxFullNameLength", Constraints.MAX_FULL_NAME_LENGTH);
        modelMap.put("maxUsernameLength", Constraints.MAX_USER_NAME_LENGTH);
        return "register";
    }
}
