package net.wanji.openx.service;

import net.wanji.common.utils.StringUtils;
import net.wanji.openx.generated.OpenScenario;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenDriveProjExtractor {
    public static void main(String[] args) {
        String address = "D:\\data\\uploadPath\\scenelib\\2023\\11\\15\\SC20231030181812ZDNU859.xosc";
        String url = "https://example.com:30080/path/to/resource";
        Pattern pattern = Pattern.compile(":(\\d{1,5})");
        Matcher matcher = pattern.matcher(url);

        while (matcher.find()) {
            String port = matcher.group(1);
            System.out.println("Port found: " + port);
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

