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

    public static class Node {
        int index;
        int weight;
    }

    Node[] nodes;
    //index数组中存放的是nodes数组的下标, Node里的index字段存放的是index数组的下标, 相互索引
    int[] leaf_index = new int[num_alphabets];//存放叶子节点的nodes索引
    int[] parent_index;//存放合并而成的空节点的nodes索引
    int num_nodes;//节点个数

    public void decode(String in, String out) {

    }

    public void encode(String in, String out) throws IOException {
        File fin = new File(in);
        FileInputStream fsin = new FileInputStream(fin);
        File fout = new File(out);
        FileOutputStream fsout = new FileOutputStream(fout);
        determineFrequency(fsin);
        //统计词频后，编码时要从头开始读，这里直接重新创建省事
        fsin.close();
        fsin = new FileInputStream(fin);
        allocateTree();
        addLeaves();

        fsin.close();
        fsout.close();
    }

    //统计词频
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

    //leaves_index不依赖num_active，最大不过MAX_BUFFER_SIZE
    void allocateTree() {
        nodes = new Node[2*num_active];
        parent_index = new int[num_active];
    }

    //初始化叶子节点
    void addLeaves() {
        int freq, i;
        for (i = 0; i < num_alphabets; i++) {
            freq = frequency[i];
            if (freq > 0) {
                add_node(-(i + 1), freq);
            }
        }

    }

    //为了标识叶子节点，叶子节点的index设置成负数，用来做区分;
    int add_node(int index, int weight) {
        int i = num_nodes++;
        //这里按顺序插入，保持weight从小到大的顺序
        while (i > 0 && nodes[i] != null && nodes[i].weight > weight) {
            nodes[i + 1] = nodes[i];
            if (nodes[i].index < 0)
                ++leaf_index[-nodes[i].index];
            else
                ++parent_index[nodes[i].index];
            --i;
        }

        ++i;
        nodes[i] = new Node();
        nodes[i].index = index;
        nodes[i].weight = weight;
        if (index < 0)
            leaf_index[-index] = i;
        else
            parent_index[index] = i;

        return i;
    }
}
