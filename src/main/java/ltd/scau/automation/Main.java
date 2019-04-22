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

    private static final int THREAD_COUNT = 8;

    private static final int TARGET_COUNT = 50;

    private static final Set<String> set = Collections.synchronizedSet(new HashSet<>());

    public static final Boolean HEADLESS = true;

    public static final Boolean USE_PROXY = false;

    //        public static final String URL = "https://www.wjx.cn/m/35825251.aspx";
//    public static final String URL = "https://www.wjx.cn/m/35859022.aspx";
//    public static final String URL = "https://www.wjx.cn/m/36025830.aspx";
    public static final String URL = "https://www.wjx.cn/m/37426354.aspx";

    private static final String WJX_SUCCESS = "WJX_SUCCESS";

    public static void main(String[] args) throws Exception {
        Matcher m = CODE_PATTERN.matcher(URL);
        String code = null;
        if (m.find()) {
            code = m.group(1);
        } else {
            throw new NullPointerException("Code cannot be null.");
        }
        parseRules(Main.class.getResource(String.format("/%s.properties", code)).getFile());

        if (USE_PROXY) {
            ProxyGetter proxyGetter = new ProxyGetter();
            Thread t = new Thread(proxyGetter);
            t.start();
        }

        Runnable runnable = () -> {
            Jedis jedis = new Jedis(URI.create("redis://:sudo%20reboot@127.0.0.1:6379/0"));
            Http http = new Http();
            for (; ; ) {
                Long wjxSuccess = jedis.llen(WJX_SUCCESS);
                if (wjxSuccess >= TARGET_COUNT) {
                    break;
                }
                String proxyIp = USE_PROXY ? jedis.brpop(Integer.MAX_VALUE, "proxy_ip").get(1) : null;
//                if (set.contains(proxyIp)) {
//                    continue;
//                }
//                set.add(proxyIp);
                System.out.println(Thread.currentThread().getName() + " Get proxy: " + proxyIp);

                try {
                    if (startDriver(proxyIp)) {
                        jedis.rpush(WJX_SUCCESS, Optional.ofNullable(proxyIp).orElse("null"));
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
        executorService.shutdown();
    }

    public static boolean startDriver(String proxyIp) {
        ChromeDriver driver = null;
        try {
            driver = proxyIp != null ? getChromeDriverWithProxy(proxyIp) : getChromeDriver();
            driver.get(URL);

            List<Question> questions = getQuestions(driver);

            questions.forEach(Main::click);

            randomSleep(1000, 1000);
            driver.findElementById("ctlNext").click();
            return true;
        } finally {
            driver.quit();
        }
    }

    public static List<Question> getQuestions(ChromeDriver driver) {
        List<WebElement> fieldElements = driver.findElementsByClassName("field");
        return fieldElements.stream().map(e -> {
            Integer topic = Integer.valueOf(e.getAttribute("topic"));
            Integer minValue = Integer.valueOf(Optional.ofNullable(e.getAttribute("minvalue")).orElse("1"));
            Integer maxValue = Integer.valueOf(Optional.ofNullable(e.getAttribute("maxvalue")).orElse("0"));
            QuestionType questionType = QuestionType.byNum(Integer.valueOf(e.getAttribute("type")));
            return Question.aQuestion()
                    .withTopic(topic)
                    .withMinValue(minValue)
                    .withMaxValue(maxValue)
                    .withType(questionType)
                    .withRequired("1".equals(e.getAttribute("req")))
                    .withClickableElements(e.findElements(By.tagName("a")))
                    .build();
        }).collect(Collectors.toList());
    }

    public static ChromeDriver getChromeDriver() {
        ChromeOptions chromeOptions = new ChromeOptions();
        if (HEADLESS) {
            chromeOptions.addArguments("headless");
        }
        chromeOptions.setCapability(CapabilityType.ForSeleniumServer.AVOIDING_PROXY, true);
        chromeOptions.setCapability(CapabilityType.ForSeleniumServer.ONLY_PROXYING_SELENIUM_TRAFFIC, true);
        ChromeDriver chromeDriver = new ChromeDriver(chromeOptions);
        chromeDriver.manage().timeouts().pageLoadTimeout(20, TimeUnit.SECONDS);
        chromeDriver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        return chromeDriver;
    }

    public static ChromeDriver getChromeDriverWithProxy(String proxyIp) {
        ChromeOptions chromeOptions = new ChromeOptions();
        if (HEADLESS) {
            chromeOptions.addArguments("headless");
        }
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
        Double total = 0.0;
        for (int i = 0; i < elements.size(); i++) {
            Double rate = topicRules.getOrDefault(i, 1.0);
            total += rate;
            scores.putIfAbsent(rate * Math.random(), i);
        }
        randomSleep(1000, 500);
        switch (question.getType()) {
            case SINGLE_SELECT:
            case RATE:
            case SCORE_SELECT:
                singleSelect(scores, elements);
                break;
            case MULTI_SELECT:
                multiSelect(scores, elements, question.getMinValue(), question.getMaxValue(), total);
                break;
            default:
        }
    }

    public static void singleSelect(TreeMap<Double, Integer> scores, List<WebElement> elements) {
        Integer highest = scores.lastEntry().getValue();
        elements.get(highest).click();
    }

    public static void multiSelect(TreeMap<Double, Integer> scores, List<WebElement> elements, Integer minValue, Integer maxValue, Double total) {
        TreeMap<Double, Integer> counts = new TreeMap<>();
        int recommendCount = (int) Math.round(total / 100);
        for (int i = minValue; i < (maxValue == 0 ? elements.size() : maxValue + 1); i++) {
            counts.putIfAbsent(i == recommendCount ? Math.random() * elements.size() : Math.random(), i);
        }
        int count = counts.pollLastEntry().getValue();
        for (int i = 0; i < count; i++) {
            Map.Entry<Double, Integer> score = scores.pollLastEntry();
            if (score.getKey() > 0) {
                elements.get(score.getValue()).click();
            }
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
