import org.openqa.selenium.Dimension
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
            def newDriver = new ChromeDriver(options)
            newDriver.manage().window().setSize(new Dimension(1024, 768))
            newDriver
        }
    }

    chrome {
        driver = {
            def newDriver = new ChromeDriver()
            newDriver.manage().window().setSize(new Dimension(1024, 768))
            newDriver
        }
    }

    firefox {
        driver = {
            def newDriver = new FirefoxDriver()
            newDriver.manage().window().setSize(new Dimension(1024, 768))
            newDriver
        }
    }
}
