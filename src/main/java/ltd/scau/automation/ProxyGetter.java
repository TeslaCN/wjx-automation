package ltd.scau.automation;

import ltd.scau.automation.http.Http;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author Wu Weijie
 */
public class ProxyGetter implements Runnable {

    private Http http = new Http();
    private Jedis jedis = new Jedis(URI.create("redis://:sudo%20reboot@127.0.0.1:6379"));

    private static final String PROXY_SOURCE = "http://api3.xiguadaili.com/ip/?tid=556931475283868&num=1000&operator=1&area=珠海,佛山,中山,深圳,东莞&protocol=https";
    @Override
    public void run() {
        for (; ; ) {
            try {
                String s = http.get(PROXY_SOURCE, StandardCharsets.UTF_8);
                String[] strings = s.split("\\s+");
                jedis.rpush("proxy_ip", strings);
                Thread.sleep( 2000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
            }
        }
    }
}
