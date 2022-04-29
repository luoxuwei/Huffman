//
// Created by 罗旭维 on 2022/4/30.
//


import java.io.*;
import java.util.Arrays;

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

    //下标从1开始，因为保证了从左到右依次为频率逐渐增加，所以当构造huffman树时，从最左边开始，取两个节点构造新节点时，
    // 可以保证左节点的下标是奇数右节点是相邻的偶数的规律，这样在编码时可以通过下标值模2来判断是左还是右
    // 在构造huffman树时是以右节点下标除以2的方式计算父节点的下标，这样，(1,2)-->1,(3,4)-->2, 这与我们熟悉的构造最小堆的算法不一样，
    // 但其实因为有单独的数组parent_index来存父节点的nodes索引,实际上只要保持一致有规律统即可，就可以通过固定的算法来计算，
    // 由于构造huffman树时是可以保证是用的右节点index，所以直接除2，在编码时左右节点都要算，要有统一适用于左右节点的算法，可以用（index + 1)/2, 这样的方式,
    // 一样可以保证 (1,2)-->1, (3,4)-->2 这样构造和编码两个阶段的效果是一样的。
    Node[] nodes;
    //index数组中存放的是nodes数组的下标, Node里的index字段存放的是index数组的下标, 相互索引
    int[] leaf_index = new int[num_alphabets];//存放叶子节点的nodes索引
    int[] parent_index;//存放合并而成的空节点的nodes索引
    int num_nodes;//节点个数

    int bits_in_buffer = 0;//编码时写的时候用于定位，解码时用于保存buffer中的总位数
    byte[] buffer = new byte[MAX_BUFFER_SIZE];
    int current_bit = 0;//解码时读的时候用
    boolean eof_input = false;//读的时候用

    public void decode(String in, String out) throws IOException {
        File fin = new File(in);
        FileInputStream fsin = new FileInputStream(fin);
        File fout = new File(out);
        FileOutputStream fsout = new FileOutputStream(fout);
        readHeader(fsin);
        buildTree();
        decodeBitStream(fsin, fsout);
        fsin.close();
        fsout.close();
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
        writeHeader(fsout);
        buildTree();
        int c;
        while ((c = fsin.read()) != -1) {
            encodeAlphabet(fsout, c);
        }
        flushBuffer(fsout);

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

    //构造Huffman树时，左节点的index是奇数，右节点index是偶数，所以index%2算出来是左1右0, readBit是1表示是左边0表示右边，所以用index*2-bit来算子节点index.
    void decodeBitStream(InputStream fin, OutputStream fo) throws IOException {
        int node_index = nodes[num_nodes].index;//根节点
        int bit = 0;
        int i = 0;
        while (true) {
            bit = readBit(fin);
            if (bit == END_OF_FILE) {
                break;
            }
            node_index = nodes[node_index*2-bit].index;
            //小于0表示到了叶子节点
            if (node_index < 0) {
                char c = (char) (-node_index - 1);
                fo.write(c);
                //如果全部解码完了就退出
                if (++i == original_size)
                    break;
                //解码完一个字符，重新从根节点出发
                node_index = nodes[num_nodes].index;
            }
        }
    }

    int readBit(InputStream in) throws IOException {
        //一个buffer读完，重新从文件中读取一个buffer
        if (current_bit == bits_in_buffer) {
            if (eof_input)
                return END_OF_FILE;
            else {
                int size = in.read(buffer);
                if (size < MAX_BUFFER_SIZE) {
                    eof_input = true;
                }
                bits_in_buffer = size << 3;//一个byte有8位，总位数是size*8
                current_bit = 0;
            }
        }

        if (bits_in_buffer == 0) return END_OF_FILE;
        int bit = (buffer[current_bit>>3] >> (7 - current_bit%8)) & 0x1;
        current_bit++;
        return bit;
    }

    //nodes是从1开始存放的，所以实际上最右边的节点下标是num_nodes, buildTree只跟parent_index有关，遍历的时候也是一样，leaf_index只是记录了一下所有叶子节点，用于编码时回溯路径
    void buildTree() {
        int a = 0, b = 0, freeindex = 1, index = 0;
        while (freeindex < num_nodes) {
            a = freeindex++;
            b = freeindex++;
            index = add_node(b/2, nodes[a].weight + nodes[b].weight);
            parent_index[b/2] = index;
        }
    }

    // 由于 buildTree() 构造huffman树时是可以保证是用的右节点index，所以直接除2，在编码时左右节点都要算，要有统一适用于左右节点的算法，可以用（index + 1)/2, 这样的方式,
    // 一样可以保证 (1,2)-->1, (3,4)-->2 这样构造和编码两个阶段的效果是一样的。构造Huffman树时，左节点的index是奇数，右节点index是偶数，所以这里算出来是左1右0,对应在decodeBitStream中，readBit是1表示是左边，正好index*2-bit是奇数.
    void encodeAlphabet(OutputStream out, int c) throws IOException {
        int nodes_index = leaf_index[c + 1];
        int stack_top = 0;
        int[] stack = new int[num_active];
        while (nodes_index < num_nodes) {
            stack[stack_top++] = nodes_index%2;
            nodes_index = parent_index[(nodes_index + 1)/2];
        }
        while (--stack_top >= 0) {
            writeBit(out, stack[stack_top]);
        }
    }

    //类似位图的实现方式，底层用byte数组，在buff中定位到某一位bit的方式是，bit/8算出byte数组的 index，bit%8算byte中的某一位
    int writeBit(OutputStream out, int bit) throws IOException {
        //buffer写满了
        if (bits_in_buffer == MAX_BUFFER_SIZE<<3) {
            out.write(buffer);
            Arrays.fill(buffer, (byte) 0);
            bits_in_buffer = 0;
        }

        if (bit == 1) {
            buffer[bits_in_buffer >> 3] |= (0x1 << (7 - bits_in_buffer%8));//从左至右的顺序，需要从低位开始算，比如求出是第3位应该是 00010000 而不是00001000
        }
        bits_in_buffer++;
        return 0;
    }

    int flushBuffer(OutputStream out) throws IOException {
        if (bits_in_buffer > 0) {
            out.write(buffer, 0,
                    (bits_in_buffer + 7) >> 3);
            bits_in_buffer = 0;
        }
        return 0;
    }


    //|original_size|num_active|{c|weight}|{c|weight}|....
    int writeHeader(OutputStream out) throws IOException {
        int size = 4 + 1 + num_active * (1 + 4);
        int weight = 0;
        byte[] buffer = new byte[size];

        int pos = 0;
        int j = 4;
        //高位存低地址，original_size >> (3*2*2*2 == 24),(2*2*2*2 == 16),8,0
        while (j > 0) {
            j--;
            buffer[pos++] = (byte) ((original_size >> (j<<3)) & 0xff);
        }

        buffer[pos++] = (byte) num_active;

        for (int i = 1; i <= num_active; i++) {
            weight = nodes[i].weight;
            buffer[pos++] = (byte) (-nodes[i].index - 1);
            j = 4;
            while (j > 0) {
                j--;
                buffer[pos++] = (byte) ((weight >> (j<<3)) & 0xff);
            }
        }
        out.write(buffer);
        return 0;
    }

    int readHeader(InputStream in) throws IOException {
        byte[] buff = new byte[4];
        int bytes_read = in.read(buff);
        if (bytes_read < 1) {
            return -1;
        }

        int pos = 0;
        original_size = buff[pos++];
        while (pos < 4) {
            original_size = (original_size << 8) | buff[pos++];
        }

        bytes_read = in.read();
        if (bytes_read < 1) {
            return -1;
        }
        num_active = bytes_read;

        allocateTree();

        int size = num_active * (1 + 4);
        buff = new byte[size];

        in.read(buff);
        pos = 0;
        int weight = 0;
        for (int i = 1; i <= num_active; i++) {
            nodes[i] = new Node();
            nodes[i].index = -(buff[pos++] + 1);
            weight = buff[pos++];
            int j = 0;
            while (++j < 4) {
                weight = (weight << 8) | buff[pos++];
            }
            nodes[i].weight = weight;
        }
        num_nodes = num_active;
        return 0;
    }

}
