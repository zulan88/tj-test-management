package net.wanji.openx.service;

import net.wanji.common.utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenDriveProjExtractor {
    public static void main(String[] args) {
//        try {
//            // 指定OpenDRIVE文件路径
//            String filePath = "C:\\Users\\wanji\\Desktop\\jttest文件\\同济大学环路.xodr";
//            File xodrfile = new java.io.File(filePath);
//            String res = StringUtils.substringAfterLast(filePath,java.io.File.separator);
//            System.out.println(res);
//            // 读取文件内容
//            BufferedReader reader = new BufferedReader(new FileReader(filePath));
//            StringBuilder fileContent = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                fileContent.append(line);
//            }
//            reader.close();
//
//            // 使用正则表达式提取proj参数值
//            String regex = "\\+proj=[^\\s]+.*?\\]";
//            Pattern pattern = Pattern.compile(regex);
//            Matcher matcher = pattern.matcher(fileContent.toString());
//
//            if (matcher.find()) {
//                // 获取匹配到的proj参数值
//                String projValue = matcher.group();
//
//                // 去掉末尾的"]"
//                projValue = projValue.substring(0, projValue.length() - 1);
//
//                // 打印proj参数值
//                System.out.println("proj参数值: " + projValue);
//            } else {
//                System.out.println("未找到proj参数");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        System.out.println(getMaxRepetitions("abab", 11, "abba", 1));
    }

    public static int getMaxRepetitions(String s1, int n1, String s2, int n2) {
        if (n1 == 0) {
            return 0;
        }
        int s1cnt = 0, index = 0, s2cnt = 0;
        // recall 是我们用来找循环节的变量，它是一个哈希映射
        // 我们如何找循环节？假设我们遍历了 s1cnt 个 s1，此时匹配到了第 s2cnt 个 s2 中的第 index 个字符
        // 如果我们之前遍历了 s1cnt' 个 s1 时，匹配到的是第 s2cnt' 个 s2 中同样的第 index 个字符，那么就有循环节了
        // 我们用 (s1cnt', s2cnt', index) 和 (s1cnt, s2cnt, index) 表示两次包含相同 index 的匹配结果
        // 那么哈希映射中的键就是 index，值就是 (s1cnt', s2cnt') 这个二元组
        // 循环节就是；
        //    - 前 s1cnt' 个 s1 包含了 s2cnt' 个 s2
        //    - 以后的每 (s1cnt - s1cnt') 个 s1 包含了 (s2cnt - s2cnt') 个 s2
        // 那么还会剩下 (n1 - s1cnt') % (s1cnt - s1cnt') 个 s1, 我们对这些与 s2 进行暴力匹配
        // 注意 s2 要从第 index 个字符开始匹配
        Map<Integer, int[]> recall = new HashMap<Integer, int[]>();
        int[] preLoop = new int[2];
        int[] inLoop = new int[2];
        while (true) {
            // 我们多遍历一个 s1，看看能不能找到循环节
            ++s1cnt;
            for (int i = 0; i < s1.length(); ++i) {
                char ch = s1.charAt(i);
                if (ch == s2.charAt(index)) {
                    index += 1;
                    if (index == s2.length()) {
                        ++s2cnt;
                        index = 0;
                    }
                }
            }
            // 还没有找到循环节，所有的 s1 就用完了
            if (s1cnt == n1) {
                return s2cnt / n2;
            }
            // 出现了之前的 index，表示找到了循环节
            if (recall.containsKey(index)) {
                int[] value = recall.get(index);
                int s1cntPrime = value[0];
                int s2cntPrime = value[1];
                // 前 s1cnt' 个 s1 包含了 s2cnt' 个 s2
                preLoop = new int[]{s1cntPrime, s2cntPrime};
                // 以后的每 (s1cnt - s1cnt') 个 s1 包含了 (s2cnt - s2cnt') 个 s2
                inLoop = new int[]{s1cnt - s1cntPrime, s2cnt - s2cntPrime};
                break;
            } else {
                recall.put(index, new int[]{s1cnt, s2cnt});
            }
        }
        // ans 存储的是 S1 包含的 s2 的数量，考虑的之前的 preLoop 和 inLoop
        int ans = preLoop[1] + (n1 - preLoop[0]) / inLoop[0] * inLoop[1];
        // S1 的末尾还剩下一些 s1，我们暴力进行匹配
        int rest = (n1 - preLoop[0]) % inLoop[0];
        for (int i = 0; i < rest; ++i) {
            for (int j = 0; j < s1.length(); ++j) {
                char ch = s1.charAt(j);
                if (ch == s2.charAt(index)) {
                    ++index;
                    if (index == s2.length()) {
                        ++ans;
                        index = 0;
                    }
                }
            }
        }
        // S1 包含 ans 个 s2，那么就包含 ans / n2 个 S2
        return ans / n2;
    }

}

