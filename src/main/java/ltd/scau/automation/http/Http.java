package ltd.scau.automation.http;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Wu Weijie
 */
public class Http {

    private HttpClient httpClient;

    public Http() {
        httpClient = HttpClients.createDefault();
    }

    public String get(String url, Charset charset) throws IOException {
        HttpResponse httpResponse = httpClient.execute(new HttpGet(url));
        return EntityUtils.toString(httpResponse.getEntity(), charset);
    }

    public String get(String url, Charset charset, HttpHost proxy) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
        httpGet.setConfig(config);
        HttpResponse response = httpClient.execute(httpGet);
        return EntityUtils.toString(response.getEntity(), charset);
    }

    public static void main(String[] args) throws Exception {
        Http http = new Http();
        String proxyIp;
        proxyIp = "115.223.100.31:8060";
//        proxyIp = "115.223.100.31:8060";
        String[] split = proxyIp.split(":");
        String response = http.get("https://www.wjx.cn", StandardCharsets.UTF_8, new HttpHost(split[0], Integer.valueOf(split[1]), "http"));
        System.out.println(response);
    }
}
