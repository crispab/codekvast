package sample.springboot.executable;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(method = GET)
@Slf4j
@SuppressWarnings("SameReturnValue")
public class HomeController {

  private final ButtonOneService buttonOneService;
  private final ButtonTwoService buttonTwoService;

  @Autowired
  public HomeController(ButtonOneService buttonOneService, ButtonTwoService buttonTwoService) {
    this.buttonOneService = buttonOneService;
    this.buttonTwoService = buttonTwoService;
  }

  @RequestMapping(value = "/", method = GET)
  String home() {
    log.info("Welcome home.");
    return "/home.html";
  }

  @RequestMapping("/button1")
  String buttonOne() {
    buttonOneService.doSomething();
    return "/buttonOne.html";
  }

  @RequestMapping("/button2")
  String buttonTwo() {
    buttonTwoService.doSomething();
    return "/buttonTwo.html";
  }
}
