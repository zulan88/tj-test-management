package net.wanji.openx.service;

import net.wanji.common.utils.StringUtils;
import net.wanji.openx.generated.OpenScenario;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenDriveProjExtractor {
    public static void main(String[] args) throws IOException {
        String address = "D:\\data\\uploadPath\\scenelib\\tjtest.xodr";
        String online = "C:\\Users\\wanji\\Downloads\\tjtest.xodr";
        File xodrfile = new java.io.File(online);
        String c1 = StringUtils.substringAfterLast(online, java.io.File.separator);
        BufferedReader reader = new BufferedReader(new FileReader(online));
        StringBuilder fileContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            fileContent.append(line);
        }
        reader.close();
        // 使用正则表达式提取proj参数值
        String regex = "\\+proj=[^\\s]+.*?\\]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(fileContent.toString());
        if (matcher.find()) {
            String projValue = matcher.group();
            String proj = projValue.substring(0, projValue.length() - 1);
            System.out.println("proj参数值：" + proj);
        } else {
            //异常处理
            System.out.println("未提取到proj参数");
        }
    }

    public static void trackentry(String address) {
        File file = new File(address);
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(OpenScenario.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            OpenScenario openScenario = (OpenScenario) jaxbUnmarshaller.unmarshal(file);

        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}

