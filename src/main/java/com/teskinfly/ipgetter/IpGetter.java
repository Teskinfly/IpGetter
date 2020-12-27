package com.teskinfly.ipgetter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
class IpGetterThread implements Runnable{
    ArrayList<String> domains;
    int startIndex;
    int endIndex;
    CountDownLatch latch;
    IpGetterThread (ArrayList<String> domains, int startIndex, int endIndex, CountDownLatch countDownLatch) {
        this.domains = domains;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        latch = countDownLatch;
    }
    public void run() {
        for (int i = startIndex; i < endIndex; i++) {
//            System.out.println(Thread.currentThread().getName());
            String domain = domains.get(i);
            String url = getUrl(domain);
            String ipAndDomain = getIpAndDomain(url, domain, 0);
            domains.set(i,ipAndDomain);
//            if (ipAndDomain == null) System.out.println("url::"+url);
            System.out.println(Thread.currentThread().getName()+"---"+ipAndDomain);
        }
        latch.countDown();
        System.out.println(Thread.currentThread().getName()+"执行完毕，还剩下线程数："+latch.getCount());
    }
    public String getUrl(String domain) {//根据域名解析出ipaddress对应的网址
        String[] parts = domain.split("\\.");//正则表达式
        String url = null;
        if (parts.length >= 3) {//二级域名以上
            if (parts.length > 3)
                url = IpGetter.PART1+parts[parts.length-2]+"."+parts[parts.length-1]+"."+ IpGetter.PART2+domain;
            else
                url = IpGetter.PART1+parts[1]+"."+parts[2]+"."+ IpGetter.PART2+domain;
        }
        else if (parts.length == 2){//一级域名
            url = IpGetter.PART1+domain+"."+ IpGetter.PART2;
        }
        return url;
    }
    public String getIpAndDomain(String url, String domain, int cnt) {//去ipaddress得到ip与域名
        if (url == null|| cnt >= 10) return null; //尝试10次以上，放弃
//        return url+" "+domain;
        Document document = null;
        try {
            document = Jsoup.connect(url).get();
            Element panel = document.getElementsByClass("panel").first();
            Element first = panel.getElementsByClass("comma-separated").first();
            if (first == null) {
                first = document.getElementsByClass("comma-separated").first();
                if (first == null) return null;
            }
            Elements li = first.getElementsByTag("li");
            String ip = li.first().text();
            return isIp(ip)?(ip + " " + domain):null;
        } catch (IOException e) {
            //超时重试机制，可以提高映射数量
            try {
                TimeUnit.MILLISECONDS.sleep(600);
            } catch (InterruptedException e2) {
            }
            String ipAndDomain = getIpAndDomain(url, domain,cnt+1);
            return ipAndDomain;
        }
    }
    public boolean isIp(String ip) {//由于解析出来的字符串要不就是域名，要不就是ip，既不需要太复杂的判断
        return (ip.charAt(0) > '0'&&ip.charAt(0) <= '9')?true:false;
    }
}
public class IpGetter {
    public static final String PART1 = "https://";
    public static final String PART2 = "ipaddress.com/";
    private static final String DOMAINS = "domains.txt";
    private static final String OUTPUT = "host.txt";
//    private static final String DOMAINS = "G:\\TestFile\\domains.txt";
//    private static final String OUTPUT = "G:\\TestFile\\host.txt";
    private static int PoolSize = 500;
    public static void main(String[] args) throws IOException {
//        getIpMapping();已废弃
        long start = System.currentTimeMillis(),end;
        ExecutorService executorService = Executors.newFixedThreadPool(PoolSize);
        CountDownLatch countDownLatch = null;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(DOMAINS)));
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(OUTPUT)));
        ArrayList<String> arrayList = new ArrayList<String>();
        Collections.synchronizedList(arrayList);//保证线程安全
        String s;
        while ((s = bufferedReader.readLine()) != null) {
            arrayList.add(s);
        }
        int maxLen = arrayList.size();
        if (maxLen < PoolSize) PoolSize = maxLen;
        countDownLatch = new CountDownLatch(PoolSize);
        int threadLen = maxLen/(PoolSize-1);
        int startIndex = 0;
        for (int i = 0; i < PoolSize; i++) {
            executorService.execute(new IpGetterThread(arrayList,startIndex,Math.min(threadLen+startIndex,maxLen),countDownLatch));
            startIndex = startIndex+threadLen;
        }
        try {
            countDownLatch.await();
            executorService.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < maxLen; i++) {
            String ipAndDomain = arrayList.get(i);
            if (ipAndDomain == null) continue;
            bufferedWriter.write(ipAndDomain);
            bufferedWriter.newLine();
        }
        end = System.currentTimeMillis();
        System.out.println("用时："+formatDuring(end-start));

    }
    public static String formatDuring(long mss) {
        long days = mss / (1000 * 60 * 60 * 24);
        long hours = (mss % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (mss % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (mss % (1000 * 60)) / 1000;
        return days + " 天 " + hours + " 小时 " + minutes + " 分钟 "
                + seconds + " 秒 ";
    }

//    1.0 版本
    @Deprecated
    public static void getIpMapping() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(DOMAINS)));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(OUTPUT)));
        String next;
        while ((next = br.readLine())!=null) {
            String[] parts = next.split("\\.");//正则表达式
            String url;
            if (parts.length >= 3) {//二级域名以上
                if (parts.length > 3)
                    url = PART1+parts[parts.length-2]+"."+parts[parts.length-1]+"."+PART2+next;
                else
                    url = PART1+parts[1]+"."+parts[2]+"."+PART2+next;
            }
            else if (parts.length == 2){//一级域名
                url = PART1+next+"."+PART2;
            }
            else {
                continue;
            }
            System.out.println(url);
            Document document = null;
            try {
                document = Jsoup.connect(url).get();
                Element first = document.getElementsByClass("comma-separated").first();
                Elements li = first.getElementsByTag("li");
                String text = li.first().text();
                text = text + " " + next;
                System.out.println(text);
                bw.write(text);
                bw.newLine();
                bw.flush();
            } catch (IOException e) {
                System.out.println(url+"获取出错");
            }
            try {
                TimeUnit.MILLISECONDS.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
