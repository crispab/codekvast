import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
// import org.openqa.selenium.Proxy

environments {

    chromeHeadless {
        driver = {
            ChromeOptions options = new ChromeOptions()
            options.addArguments('headless')
//            Proxy proxy = new Proxy()
//            proxy.setHttpProxy("localhost:8888")
//            options.setCapability("proxy", proxy)
            new ChromeDriver(options)
        }
    }

    chrome {
        driver = {
//            ChromeOptions options = new ChromeOptions()
//            options.addArguments('headless')
//            Proxy proxy = new Proxy()
//            proxy.setHttpProxy("localhost:8888")
//            options.setCapability("proxy", proxy)
            new ChromeDriver()
        }
    }

    firefox {
        driver = {
            new FirefoxDriver()
        }
    }
}
