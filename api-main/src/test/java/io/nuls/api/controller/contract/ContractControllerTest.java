package io.nuls.api.controller.contract;

import io.nuls.api.ApiModuleBootstrap;
import io.nuls.api.bean.SpringLiteContext;
import io.nuls.api.controller.model.RpcResult;
import io.nuls.api.core.util.Log;
import io.nuls.sdk.core.utils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ContractControllerTest {

    private static String BASE;

    private static void getBase() {
        String serverHome = System.getProperty("api.server.home");
        if(StringUtils.isBlank(serverHome)) {
            URL resource = ClassLoader.getSystemClassLoader().getResource("");
            String classPath = resource.getPath();
            File file = null;
            try {
                file = new File(URLDecoder.decode(classPath, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                Log.error(e);
                file = new File(classPath);
            }
            BASE = file.getPath();
        } else {
            BASE = serverHome;
        }
    }

    @BeforeClass
    public static void startUp() {
        getBase();
        ApiModuleBootstrap.main(null);
    }

    @Test
    public void validateContractCode() {
        FileInputStream in=  null;
        try {
            TimeUnit.SECONDS.sleep(15);
            ContractController controller = SpringLiteContext.getBean(ContractController.class);
            List<Object> params = new ArrayList<>();
            String address = "TTbB7CA2q6QjMdiUUbLGs85nHrhvwjga";
            File file = new File(BASE + "/contract/code/ddd.zip");
            in = new FileInputStream(file);
            params.add(address);
            params.add("mockHeader," + Base64.getEncoder().encodeToString(IOUtils.toByteArray(in)));
            RpcResult rpcResult = controller.validateContractCode(params);
            System.out.println(rpcResult.getResult());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in);
        }

    }

    @Test
    public void testFileBinaryBase64Encoder() {
        FileInputStream in = null;
        OutputStream out = null;
        try {
            File file = new File(BASE + "/contract/code/ddd.zip");
            in = new FileInputStream(file);

            String fileDataURL = "aaaa,";
            fileDataURL += Base64.getEncoder().encodeToString(IOUtils.toByteArray(in));
            String[] arr = fileDataURL.split(",");
            if (arr.length != 2) {
                Assert.assertTrue(false);
            }
            String headerInfo = arr[0];
            String body = arr[1];

            //String contentType = (headerInfo.split(":")[1]).split(";")[0];
            byte[] fileContent = Base64.getDecoder().decode(body);
            out = new FileOutputStream(new File(BASE + "/contract/code/ddd_copy.zip"));
            IOUtils.write(fileContent, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

    @Test
    public void getCodeTest() {
        File file = new File("/Users/pierreluo/IdeaProjects/api-module/api-main/target/test-classes/contract/code/TTbB7CA2q6QjMdiUUbLGs85nHrhvwjga/src/io/nuls/vote/contract/VoteContract.java");
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            List<String> strings = IOUtils.readLines(in);
            StringBuilder sb = new StringBuilder();
            strings.forEach(a -> {
                sb.append(a).append("\r\n");
            });
            System.out.println(sb.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in);
        }
    }


}