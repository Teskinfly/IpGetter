import com.teskinfly.ipgetter.IpGetter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class JsoupDemo {
    public static String getIpAndDomain(String url, String domain) {
        if (url == null) return null;
//        return url+" "+domain;
        Document document = null;
        try {
            document = Jsoup.connect(url).get();
            Element panel = document.getElementsByClass("panel").first();
            Element first = panel.getElementsByClass("comma-separated").first();
            if (first == null) return null;
            Elements li = first.getElementsByTag("li");
            String text = li.first().text();
            text = text + " " + domain;
            return text;
        } catch (IOException e) {
            System.out.println(url+"获取出错");
        }
        return null;
    }
    public static void main(String[] args)  {
//        Document document = null;
//        String ipAndDomain = getIpAndDomain("https://google.com.ipaddress.com/r2---sn-n4v7sn7l.c.video.google.com", "0");
//        System.out.println(ipAndDomain);
        System.out.println(IpGetter.formatDuring(23423423));
    }
}
