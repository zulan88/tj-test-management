package net.wanji.web.controller.common;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.ApiOperation;
import net.wanji.business.common.Constants.DefaultSign;
import net.wanji.common.config.WanjiConfig;
import net.wanji.common.config.WanjiConfig;
import net.wanji.common.constant.Constants;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.enums.DataSourceType;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.file.FileUploadUtils;
import net.wanji.common.utils.file.FileUtils;
import net.wanji.framework.config.ServerConfig;
import net.wanji.framework.datasource.DynamicDataSourceContextHolder;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用请求处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/common")
public class CommonController {
    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    @Autowired
    private ServerConfig serverConfig;


    private static final String FILE_DELIMETER = ",";

    /**
     * 通用下载请求
     *
     * @param fileName 文件名称
     * @param delete   是否删除
     */
    @GetMapping("/download")
    public void fileDownload(String fileName, Boolean delete, HttpServletResponse response, HttpServletRequest request) {
        try {
            if (!FileUtils.checkAllowDownload(fileName)) {
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }
            String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
            String filePath = WanjiConfig.getDownloadPath() + fileName;

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (delete) {
                FileUtils.deleteFile(filePath);
            }
        } catch (Exception e) {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 通用上传请求（单个）
     */
    @ApiOperation("上传")
    @PostMapping("/upload")
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        try {
            // 上传文件路径
            String filePath = WanjiConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
    /**
     * 通用上传请求（单个）
     */
    @ApiOperation("上传Excel")
    @PostMapping("/uploadExcel")
    public AjaxResult uploadExcel(@RequestParam("file") MultipartFile file) throws Exception {
        try {
            // 上传文件路径
            String filePath = WanjiConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.uploadExcel(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }




    /**
     * 通用上传请求（多个）
     */
    @PostMapping("/uploads")
    public AjaxResult uploadFiles(List<MultipartFile> files) throws Exception {
        try {
            // 上传文件路径
            String filePath = WanjiConfig.getUploadPath();
            List<String> urls = new ArrayList<String>();
            List<String> fileNames = new ArrayList<String>();
            List<String> newFileNames = new ArrayList<String>();
            List<String> originalFilenames = new ArrayList<String>();
            for (MultipartFile file : files) {
                // 上传并返回新文件名称
                String fileName = FileUploadUtils.upload(filePath, file);
                String url = serverConfig.getUrl() + fileName;
                urls.add(url);
                fileNames.add(fileName);
                newFileNames.add(FileUtils.getName(fileName));
                originalFilenames.add(file.getOriginalFilename());
            }
            AjaxResult ajax = AjaxResult.success();
            ajax.put("urls", StringUtils.join(urls, FILE_DELIMETER));
            ajax.put("fileNames", StringUtils.join(fileNames, FILE_DELIMETER));
            ajax.put("newFileNames", StringUtils.join(newFileNames, FILE_DELIMETER));
            ajax.put("originalFilenames", StringUtils.join(originalFilenames, FILE_DELIMETER));
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 本地资源通用下载
     */
    @PostMapping ("/download/resource")
    public void resourceDownload(String resource, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        try {
            if (!FileUtils.checkAllowDownload(resource)) {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许下载。 ", resource));
            }
            // 本地资源路径
            String localPath = WanjiConfig.getProfile();
            // 数据库资源地址
            String downloadPath = localPath + StringUtils.substringAfter(resource, Constants.RESOURCE_PREFIX);
            // 下载名称
            String downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());

        } catch (Exception e) {
            log.error("下载文件失败", e);

        }
    }

    @GetMapping ("/getDefaultSign")
    public AjaxResult getDefaultSign() {
       return AjaxResult.success(DefaultSign.getDefaultSign());
    }

    /**
     * 标注规则上传（单个）
     */
    @PostMapping("/tagupload")
    public AjaxResult taguploadFile(MultipartFile file) throws Exception {
        try {
            // 上传文件路径
            String filePath = WanjiConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            //文件下载地址连接
            ajax.put("url", url);
            //标注存储路径
            ajax.put("tagStoragePath", fileName);
            //标注文件名称
            ajax.put("tagFileName", FileUtils.getName(fileName));
            //原文件名称
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("/downloadZip")
    public void downloadFiles(HttpServletRequest request, HttpServletResponse response, String[] urls) {

        //筛选存在于数据库的图片
        String zipName = null;
        for (String url : urls) {
            String[] split = url.split("/");
            for (String s : split) {
                if (s.contains("G")) {
                    //处理文件
                     zipName = s + ".zip";
                    break;
                }
            }
        }
       
        response.setHeader("content-type", "application/octet-stream");
        response.setHeader("Content-disposition", "attachment;filename=" + zipName);
        response.setCharacterEncoding("utf-8");
        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            for (String iUrl : urls) {
                URL url = new URL(iUrl);
                String name = FilenameUtils.getName(iUrl);
                boolean rource = getRource(String.valueOf(url));
                if (!rource) {
                    continue;
                }
                switch (name) {
                    case "0.jpeg":
                        zipOut.putNextEntry(new ZipEntry("车头.jpeg"));
                        break;
                    case "1.jpeg":
                        zipOut.putNextEntry(new ZipEntry("车尾.jpeg"));
                        break;
                    case "2.jpeg":
                        zipOut.putNextEntry(new ZipEntry("车身.jpeg"));
                        break;
                    case "3.jpeg":
                        zipOut.putNextEntry(new ZipEntry("车牌.jpeg"));
                        break;
                    case "4.mp4":
                        zipOut.putNextEntry(new ZipEntry("视频.mp4"));
                        break;
                    default:
                        break;
                }
                //zipOut.putNextEntry(new ZipEntry(FilenameUtils.getName(iUrl)));

                InputStream in = new BufferedInputStream(url.openStream());
                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = in.read(buffer)) != -1) {
                    zipOut.write(buffer, 0, len);
                }
                //zipOut.write(in.readAllBytes());

                zipOut.closeEntry();
                in.close();
            }
        } catch (IOException e) {
            log.error("导出相册压缩包失败", e);
        }
    }
//判断链接是否存在
    public boolean getRource(String source) {
        try {
            URL url = new URL(source);
            URLConnection uc = url.openConnection();
            InputStream in = uc.getInputStream();
            if (source.equalsIgnoreCase(uc.getURL().toString()))
                in.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
