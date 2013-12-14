package com.smart.framework.helper;

import com.smart.framework.FrameworkConstant;
import com.smart.framework.bean.Multipart;
import com.smart.framework.util.FileUtil;
import com.smart.framework.util.StreamUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

public class UploadHelper {

    private static final Logger logger = Logger.getLogger(UploadHelper.class);

    private static final int uploadLimit = ConfigHelper.getNumberProperty(FrameworkConstant.APP_UPLOAD_LIMIT);

    private static ServletFileUpload fileUpload;

    public static void init(ServletContext servletContext) {
        File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
        fileUpload = new ServletFileUpload(new DiskFileItemFactory(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD, repository));
        if (uploadLimit != 0) {
            fileUpload.setFileSizeMax(uploadLimit * 1024 * 1024);
            if (logger.isDebugEnabled()) {
                logger.debug("[Smart] limit of uploading: " + uploadLimit + "M");
            }
        }
    }

    public static boolean isMultipart(HttpServletRequest request) {
        return ServletFileUpload.isMultipartContent(request);
    }

    public static List<Object> createMultipartParamList(HttpServletRequest request) throws Exception {
        // 定义参数列表
        List<Object> paramList = new ArrayList<Object>();
        // 创建两个对象，分别对应 普通字段 与 文件字段
        Map<String, String> fieldMap = new HashMap<String, String>();
        List<Multipart> multipartList = new ArrayList<Multipart>();
        // 获取并遍历表单项
        List<FileItem> items = fileUpload.parseRequest(request);
        for (FileItem item : items) {
            // 分两种情况处理表单项
            String fieldName = item.getFieldName();
            if (item.isFormField()) {
                // 处理普通字段
                String fieldValue = item.getString(FrameworkConstant.DEFAULT_CHARSET);
                fieldMap.put(fieldName, fieldValue);
            } else {
                // 处理文件字段
                String originalFileName = FilenameUtils.getName(item.getName());
                String uploadedFileName = FileUtil.getEncodedFileName(originalFileName);
                InputStream inputSteam = item.getInputStream();
                Multipart multipart = new Multipart(uploadedFileName, inputSteam);
                multipartList.add(multipart);
                fieldMap.put(fieldName, uploadedFileName);
            }
        }
        // 初始化参数列表
        paramList.add(fieldMap);
        if (multipartList.size() > 1) {
            paramList.add(multipartList);
        } else if (multipartList.size() == 1) {
            paramList.add(multipartList.get(0));
        } else {
            paramList.add(null);
        }
        // 返回参数列表
        return paramList;
    }

    public static void uploadFile(String basePath, Multipart multipart) {
        try {
            // 创建文件路径（绝对路径）
            String filePath = basePath + multipart.getFileName();
            FileUtil.createFile(filePath);
            // 执行流复制操作
            InputStream inputStream = new BufferedInputStream(multipart.getInputStream());
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(filePath));
            StreamUtil.copyStream(inputStream, outputStream);
        } catch (Exception e) {
            logger.error("上传文件出错！", e);
            throw new RuntimeException(e);
        }
    }
}
