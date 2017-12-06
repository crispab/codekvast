import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.phantomjs.PhantomJSDriver

environments {

    chromeHeadless {
        driver = {
            ChromeOptions o = new ChromeOptions()
            o.addArguments('headless')
            new ChromeDriver(o)
        }
    }

    chrome {
        driver = {
            new ChromeDriver()
        }
    }

    firefox {
        driver = {
            new FirefoxDriver()
        }
    }

    phantomjs {
        driver = {
            new PhantomJSDriver()
        }
    }
}
