//
// Created by 罗旭维 on 2022/4/30.
//


import java.io.*;

public class Huffman {
    public final int END_OF_FILE =  -1;
    public final int MAX_BUFFER_SIZE = 256;

    //限制只支持ASCII码
    public final int num_alphabets = 256;
    int num_active = 0;//文档里出现过的字符数
    int original_size = 0;//原始文件大小
    int[] frequency = new int[num_alphabets];

    public void decode(String in, String out) {

    }

    public void encode(String in, String out) throws IOException {
        File fin = new File(in);
        FileInputStream fsin = new FileInputStream(fin);
        File fout = new File(out);
        FileOutputStream fsout = new FileOutputStream(fout);
        determineFrequency(fsin);

        fsin.close();
        fsout.close();
    }

    void determineFrequency(InputStream in) throws IOException {
        int c;
        while ((c = in.read()) > 0) {
            frequency[c]++;
            original_size++;
        }

        for (c = 0; c < num_alphabets; ++c)
            if (frequency[c] > 0)
                ++num_active;
    }
}
