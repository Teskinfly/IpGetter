import java.io.*;

public class GenerateDomain {
    public static final String INPUT = "G:\\TestFile\\hosts";
    public static final String TARGET = "G:\\TestFile\\domains.txt";
    public static final String CLEANTARGET = "G:\\TestFile\\host.txt";
    public static final String FINAL = "G:\\TestFile\\write.txt";
    public static void main(String[] args) throws IOException {
//        generate();
        clean();
    }

    private static void clean() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(CLEANTARGET)));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FINAL)));
        String s;
        while ((s=br.readLine()) != null) {
            if (s.length() == 0 || s.charAt(0) == '#') continue;
            String[] split = s.split("\\s+");
            if (split[0].equals("github.com"))continue;
            bw.write(s);
            bw.newLine();
        }
        bw.flush();
    }

    private static void generate() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(INPUT)));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(TARGET)));
        String s;
        while ((s=br.readLine()) != null) {
            if (s.length() == 0 || s.charAt(0) == '#') continue;
            String[] split = s.split("\\s+");
            bw.write(split[1]);
            bw.newLine();
        }
        bw.flush();
    }
}
