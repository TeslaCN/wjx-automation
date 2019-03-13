package ltd.scau.automation;

import ltd.scau.automation.http.Http;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import redis.clients.jedis.Jedis;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Wu Weijie
 */
public class Main {

    public static final Map<Integer, Map<Integer, Double>> rules = new LinkedHashMap<>();

    private static final Pattern CODE_PATTERN = Pattern.compile("https://www\\.wjx\\.cn/m/(\\d+)\\.aspx");

    private static final int THREAD_COUNT = 16;

        public static final String URL = "https://www.wjx.cn/m/35825251.aspx";
//    public static final String URL = "https://www.wjx.cn/m/35859022.aspx";

    public static void main(String[] args) throws Exception {
        Matcher m = CODE_PATTERN.matcher(URL);
        String code = null;
        if (m.find()) {
            code = m.group(1);
        } else {
            throw new NullPointerException("Code cannot be null.");
        }
        parseRules(Main.class.getResource(String.format("/%s.properties", code)).getFile());

        ProxyGetter proxyGetter = new ProxyGetter();
        Thread t = new Thread(proxyGetter);
        t.start();

        Runnable runnable = () -> {
            Jedis jedis = new Jedis(URI.create("redis://:sudo%20reboot@127.0.0.1:6379/0"));
            Http http = new Http();
            for (; ; ) {
                String proxyIp = jedis.brpop(Integer.MAX_VALUE, "proxy_ip").get(1);
                System.out.println(Thread.currentThread().getName() + " Get proxy: " + proxyIp);

                String[] split = proxyIp.split(":");
                try {
                    if (startDriver(proxyIp)) {
                        jedis.rpush("wjx_success", proxyIp);
                    }
                } catch (Exception e) {
                    System.err.println("Drop: " + proxyIp);
                }

            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.execute(runnable);
        }
    }

    public static boolean startDriver(String proxyIp) {
        ChromeDriver driver = null;
        try {
            driver = getChromeDriver(proxyIp);
            driver.get(URL);

            List<WebElement> fieldElements = driver.findElementsByClassName("field");
            List<Question> questions = fieldElements.stream().map(e -> {
                Integer topic = Integer.valueOf(e.getAttribute("topic"));
                Integer minValue = Integer.valueOf(Optional.ofNullable(e.getAttribute("minvalue")).orElse("1"));
                QuestionType questionType = QuestionType.byNum(Integer.valueOf(e.getAttribute("type")));
                return Question.aQuestion()
                        .withTopic(topic)
                        .withMinValue(minValue)
                        .withType(questionType)
                        .withRequired("1".equals(e.getAttribute("req")))
                        .withClickableElements(e.findElements(By.tagName("a")))
                        .build();
            }).collect(Collectors.toList());

            questions.forEach(Main::click);

            randomSleep(1000, 1000);
            driver.findElementById("ctlNext").click();
            return true;
        } finally {
            driver.quit();
        }
    }

    public static ChromeDriver getChromeDriver(String proxyIp) {
        ChromeOptions chromeOptions = new ChromeOptions();
        Proxy proxy = new Proxy().setHttpProxy(proxyIp).setSslProxy(proxyIp);
        chromeOptions.setCapability(CapabilityType.ForSeleniumServer.AVOIDING_PROXY, true);
        chromeOptions.setCapability(CapabilityType.ForSeleniumServer.ONLY_PROXYING_SELENIUM_TRAFFIC, true);
        chromeOptions.setCapability(CapabilityType.PROXY, proxy);
        ChromeDriver chromeDriver = new ChromeDriver(chromeOptions);
        chromeDriver.manage().timeouts().pageLoadTimeout(20, TimeUnit.SECONDS);
        chromeDriver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        return chromeDriver;
    }

    public static void parseRules(String propertiesFile) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(propertiesFile)) {
            properties.load(in);
        }
        properties.stringPropertyNames().forEach(k -> {
            Double probability = Double.valueOf(properties.getProperty(k));
            String[] split = k.split("\\.");
            Integer topic = Integer.valueOf(split[0]);
            Integer option = Integer.valueOf(split[1]) - 1;
            rules.putIfAbsent(topic, new LinkedHashMap<>());
            rules.get(topic).put(option, probability);
        });
    }

    public static void click(Question question) {
        Map<Integer, Double> topicRules = rules.getOrDefault(question.getTopic(), Collections.emptyMap());
        TreeMap<Double, Integer> scores = new TreeMap<>();
        List<WebElement> elements = question.getClickableElements();
        for (int i = 0; i < elements.size(); i++) {
            scores.putIfAbsent(topicRules.getOrDefault(i, 1.0) * Math.random(), i);
        }
        randomSleep(1000, 500);
        switch (question.getType()) {
            case SINGLE_SELECT:
            case RATE:
            case SCORE_SELECT:
                singleSelect(scores, elements);
                break;
            case MULTI_SELECT:
                multiSelect(scores, elements, question.getMinValue());
                break;
            default:
        }
    }

    public static void singleSelect(TreeMap<Double, Integer> scores, List<WebElement> elements) {
        Integer highest = scores.lastEntry().getValue();
        elements.get(highest).click();
    }

    public static void multiSelect(TreeMap<Double, Integer> scores, List<WebElement> elements, Integer minValue) {
        Random random = new Random();
        int count = random.nextInt(elements.size() - minValue + 1) + minValue;
        for (int i = 0; i < count; i++) {
            elements.get(scores.pollLastEntry().getValue()).click();
            randomSleep(250, 250);
        }
    }

    private static final Random RANDOM = new Random();

    private static void randomSleep(int base, int random) {
        try {
            Thread.sleep(base + RANDOM.nextInt(random));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
