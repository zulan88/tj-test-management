package net.wanji.openx.service;

import net.wanji.common.utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenDriveProjExtractor {
    public static void main(String[] args) {
        try {
            // 指定OpenDRIVE文件路径
            String filePath = "C:\\Users\\wanji\\Desktop\\jttest文件\\同济大学环路.xodr";
            File xodrfile = new java.io.File(filePath);
            String res = StringUtils.substringAfterLast(filePath,java.io.File.separator);
            System.out.println(res);

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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

