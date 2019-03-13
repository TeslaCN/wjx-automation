package ltd.scau.automation;

import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;

/**
 * @author Wu Weijie
 */
@SuppressWarnings("ALL")
public class TestProxy {

    public static void main(String[] args) {
        ChromeOptions chromeOptions = new ChromeOptions();
        String proxyIp;
//        proxyIp = "203.42.227.113:8080";
        proxyIp = "208.98.186.80:53630";
        proxyIp = "109.86.41.111:45308";
        proxyIp = "112.85.167.230:9999";
        Proxy proxy = new Proxy().setHttpProxy(proxyIp).setSslProxy(proxyIp);
        chromeOptions.setCapability(CapabilityType.ForSeleniumServer.AVOIDING_PROXY, true);
        chromeOptions.setCapability(CapabilityType.ForSeleniumServer.ONLY_PROXYING_SELENIUM_TRAFFIC, true);
        chromeOptions.setCapability(CapabilityType.PROXY, proxy);
        ChromeDriver driver = new ChromeDriver(chromeOptions);
        driver.get("http://www.ip138.com");

        driver.quit();
    }
}
