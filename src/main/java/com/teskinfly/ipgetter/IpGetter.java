package com.teskinfly.ipgetter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class IpGetter {
    public static final String PART1 = "https://";
    public static final String PART2 = "ipaddress.com/";
    public static final String DOMAINS = "domains.txt";
    public static final String OUTPUT = "host.txt";
    public static void main(String[] args) throws IOException {
        getIpMapping();
    }
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
