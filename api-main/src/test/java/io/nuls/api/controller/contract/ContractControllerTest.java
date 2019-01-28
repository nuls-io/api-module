package io.nuls.api.controller.contract;

import io.nuls.api.ApiModuleBootstrap;
import io.nuls.api.bean.SpringLiteContext;
import io.nuls.api.controller.model.RpcResult;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ContractControllerTest {

    @BeforeClass
    public static void startUp() {
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
            File file = new File("/Users/pierreluo/Nuls/temp/code/ddd.zip");
            in = new FileInputStream(file);
            params.add(address);
            params.add(in);
            RpcResult rpcResult = controller.validateContractCode(params);
            System.out.println(rpcResult.getResult());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in);
        }

    }
}