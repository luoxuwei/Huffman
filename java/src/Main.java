//
// Created by 罗旭维 on 2022/4/30.
//

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        Huffman huffman = new Huffman();
        if (args[0].equals("e")) {
            try {
                huffman.encode(args[1], args[2]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                huffman.decode(args[1], args[2]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
