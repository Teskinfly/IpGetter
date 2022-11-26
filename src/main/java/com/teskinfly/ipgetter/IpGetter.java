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

class IpGetterThread implements Runnable {
    ArrayList<String> domains;
    int startIndex;
    int endIndex;
    CountDownLatch latch;
    final int sleepTime = 0;

    IpGetterThread(ArrayList<String> domains, int startIndex, int endIndex, CountDownLatch countDownLatch) {
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
            domains.set(i, ipAndDomain);
//            if (ipAndDomain == null) System.out.println("url::"+url);
            System.out.println(Thread.currentThread().getName() + "---" + ipAndDomain);
        }
        latch.countDown();
        System.out.println(Thread.currentThread().getName() + "执行完毕，还剩下线程数：" + latch.getCount());
    }

    //20220514测试 这个拼接方式已经过期了
//    public String getUrl(String domain) {//根据域名解析出ipaddress对应的网址
//        String[] parts = domain.split("\\.");//正则表达式
//        String url = null;
//        if (parts.length >= 3) {//二级域名以上
//            if (parts.length > 3)
//                url = IpGetter.PART1+parts[parts.length-2]+"."+parts[parts.length-1]+"."+ IpGetter.PART2+domain;
//            else
//                url = IpGetter.PART1+parts[1]+"."+parts[2]+"."+ IpGetter.PART2+domain;
//        }
//        else if (parts.length == 2){//一级域名
//            url = IpGetter.PART1+domain+"."+ IpGetter.PART2;
//        }
//        return url;
//    }
    public String getUrl(String domain) {
        return "https://ipaddress.com/site/" + domain;
    }

    public String getIpAndDomain(String url, String domain, int cnt) {//去ipaddress得到ip与域名
        if (url == null || cnt >= 10) return null; //尝试10次以上，放弃
//        return url+" "+domain;
        Document document = null;
        try {
            document = Jsoup.connect(url).header("referer", "https://api.fouanalytics.com").get(); //新版添加了不能直接访问
            Element panel = document.getElementsByClass("map-container").first();      //20221128 这里改了名称
            Element first = null;
            try {
                first = panel.getElementsByClass("comma-separated").first();
            } catch (Exception e) {
                System.out.println(url + "解析错误");
                return null;
            }
            if (first == null) {
                first = document.getElementsByClass("comma-separated").first();
                if (first == null) {
                    System.out.println(url + ": 找不到对应的要素");
                    return null;
                }
            }
            Elements li = first.getElementsByTag("li");
            String ip = li.first().text();
            if (!isIp(ip)) {
                System.out.println(url + "找到错误Ip" + ip);
            }
            return isIp(ip) ? (ip + " " + domain) : null;
        } catch (IOException e) {
            //超时重试机制，可以提高映射数量
            try {
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            } catch (InterruptedException e2) {
            }
            String ipAndDomain = getIpAndDomain(url, domain, cnt + 1);
            return ipAndDomain;
        }
    }

    public boolean isIp(String ip) {//由于解析出来的字符串要不就是域名，要不就是ip，既不需要太复杂的判断
        return (ip.charAt(0) > '0' && ip.charAt(0) <= '9') ? true : false;
    }
}

public class IpGetter {
    public static final String PART1 = "https://";
    public static final String PART2 = "ipaddress.com/";
    private static final String DOMAINS = "domains.txt";
    private static final String OUTPUT = "host.txt";
    //    private static final String DOMAINS = "D:\\BaiduNetdiskDownload\\HostGenerator\\multi_thread\\domains.txt";
//    private static final String OUTPUT = "D:\\BaiduNetdiskDownload\\HostGenerator\\multi_thread\\host.txt";
    private static int POOL_SIZE = 17;

    public static void main(String[] args) throws IOException {
//        getIpMapping();已废弃
        long start = System.currentTimeMillis(), end;
        ExecutorService executorService = Executors.newFixedThreadPool(POOL_SIZE);
        CountDownLatch countDownLatch = null;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(DOMAINS)));//输入域名
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(OUTPUT))); //输出文件
        ArrayList<String> arrayList = new ArrayList<String>();
        Collections.synchronizedList(arrayList);//保证线程安全
        String s;
        while ((s = bufferedReader.readLine()) != null) {
            arrayList.add(s);
        }
        int maxLen = arrayList.size();
        if (maxLen < POOL_SIZE) POOL_SIZE = maxLen;
        countDownLatch = new CountDownLatch(POOL_SIZE);
        int threadLen = maxLen / (POOL_SIZE - 1);
        int startIndex = 0;
        //执行获取结果
        for (int i = 0; i < POOL_SIZE; i++) {
            executorService.execute(new IpGetterThread(arrayList, startIndex, Math.min(threadLen + startIndex, maxLen), countDownLatch));
            startIndex = startIndex + threadLen;
        }
        try {
            countDownLatch.await();
            executorService.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //输出文件
        for (int i = 0; i < maxLen; i++) {
            String ipAndDomain = arrayList.get(i);
            if (ipAndDomain == null) continue;
            bufferedWriter.write(ipAndDomain);
            bufferedWriter.newLine();
        }
        bufferedWriter.flush();
        end = System.currentTimeMillis();
        System.out.println("用时：" + formatDuring(end - start));

    }

    public static String formatDuring(long mss) {
        long days = mss / (1000 * 60 * 60 * 24);
        long hours = (mss % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (mss % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (mss % (1000 * 60)) / 1000;
        return days + " 天 " + hours + " 小时 " + minutes + " 分钟 "
                + seconds + " 秒 ";
    }

//    //    1.0 版本
//    @Deprecated
//    public static void getIpMapping() throws IOException {
//        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(DOMAINS)));
//        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(OUTPUT)));
//        String next;
//        while ((next = br.readLine()) != null) {
//            String[] parts = next.split("\\.");//正则表达式
//            String url;
//            if (parts.length >= 3) {//二级域名以上
//                if (parts.length > 3)
//                    url = PART1 + parts[parts.length - 2] + "." + parts[parts.length - 1] + "." + PART2 + next;
//                else
//                    url = PART1 + parts[1] + "." + parts[2] + "." + PART2 + next;
//            } else if (parts.length == 2) {//一级域名
//                url = PART1 + next + "." + PART2;
//            } else {
//                continue;
//            }
//            System.out.println(url);
//            Document document = null;
//            try {
//                document = Jsoup.connect(url).get();
//                Element first = document.getElementsByClass("comma-separated").first();
//                Elements li = first.getElementsByTag("li");
//                String text = li.first().text();
//                text = text + " " + next;
//                System.out.println(text);
//                bw.write(text);
//                bw.newLine();
//                bw.flush();
//            } catch (IOException e) {
//                System.out.println(url + "获取出错");
//            }
//            try {
//                TimeUnit.MILLISECONDS.sleep(250);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
