/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.api.controller.contract;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Controller;
import io.nuls.api.bean.annotation.RpcMethod;
import io.nuls.api.bridge.WalletRPCHandler;
import io.nuls.api.controller.model.RpcErrorCode;
import io.nuls.api.controller.model.RpcResult;
import io.nuls.api.controller.model.RpcResultError;
import io.nuls.api.controller.utils.VerifyUtils;
import io.nuls.api.core.model.*;
import io.nuls.api.core.util.Log;
import io.nuls.api.service.ContractService;
import io.nuls.api.service.TokenService;
import io.nuls.api.utils.JsonRpcException;
import io.nuls.api.utils.RunShellUtil;
import io.nuls.contract.validation.service.CompareJar;
import io.nuls.sdk.core.model.CreateContractData;
import io.nuls.sdk.core.model.Result;
import io.nuls.sdk.core.model.transaction.CreateContractTransaction;
import io.nuls.sdk.core.utils.AddressTool;
import io.nuls.sdk.core.utils.StringUtils;
import io.nuls.sdk.tool.NulsSDKTool;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Niels
 */
@Controller
public class ContractController {

    private static String BASE;

    private static String VALIDATE_HOME;

    static {
        String serverHome = System.getProperty("api.server.home");
        if (StringUtils.isBlank(serverHome)) {
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
        VALIDATE_HOME = BASE + File.separator + "contract" + File.separator + "code" + File.separator;
    }

    @Autowired
    private WalletRPCHandler rpcHandler;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private ContractService contractService;

    @RpcMethod("getAccountTokens")
    public RpcResult getAccountTokens(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        String address = (String) params.get(2);
        if (!AddressTool.validAddress(address)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[address] is inValid"));
        }
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        PageInfo<AccountTokenInfo> pageInfo = tokenService.getAccountTokens(address, pageIndex, pageSize);
        RpcResult result = new RpcResult();
        result.setResult(pageInfo);
        return result;
    }

    @RpcMethod("getContractTokens")
    public RpcResult getContractTokens(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        String contractAddress = (String) params.get(2);
        if (!AddressTool.validAddress(contractAddress)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[contractAddress] is inValid"));
        }
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        PageInfo<AccountTokenInfo> pageInfo = tokenService.getContractTokens(contractAddress, pageIndex, pageSize);
        RpcResult result = new RpcResult();
        result.setResult(pageInfo);
        return result;
    }

    @RpcMethod("getTokenTransfers")
    public RpcResult getTokenTransfers(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        String address = (String) params.get(2);
        String contractAddress = (String) params.get(3);

        if (StringUtils.isBlank(address) && StringUtils.isBlank(contractAddress)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[address] or [contractAddress] is inValid"));
        }
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        PageInfo<TokenTransfer> pageInfo = tokenService.getTokenTransfers(address, contractAddress, pageIndex, pageSize);
        RpcResult result = new RpcResult();
        result.setResult(pageInfo);
        return result;

    }

    @RpcMethod("getContract")
    public RpcResult getContract(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        String contractAddress = (String) params.get(0);
        if (!AddressTool.validAddress(contractAddress)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[contractAddress] is inValid"));
        }
        RpcResult rpcResult = new RpcResult();
        try {
            ContractInfo contractInfo = contractService.getContractInfo(contractAddress);
            if (contractInfo == null) {
                rpcResult.setError(new RpcResultError(RpcErrorCode.DATA_NOT_EXISTS));
            } else {
                rpcResult.setResult(contractInfo);
            }
        } catch (Exception e) {
            Log.error(e);
            rpcResult.setError(new RpcResultError(RpcErrorCode.SYS_UNKNOWN_EXCEPTION));
        }
        return rpcResult;
    }

    @RpcMethod("getContractTxList")
    public RpcResult getContractTxList(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        int type = (int) params.get(2);
        String contractAddress = (String) params.get(3);

        if (!AddressTool.validAddress(contractAddress)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[contractAddress] is inValid"));
        }
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        PageInfo<ContractTxInfo> pageInfo = contractService.getContractTxList(contractAddress, type, pageIndex, pageSize);
        RpcResult result = new RpcResult();
        result.setResult(pageInfo);
        return result;
    }

    @RpcMethod("getContractList")
    public RpcResult getContractList(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        boolean onlyNrc20 = (boolean) params.get(2);
        boolean isHidden = (boolean) params.get(3);
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        PageInfo<ContractInfo> pageInfo = contractService.getContractList(pageIndex, pageSize, onlyNrc20, isHidden);
        RpcResult result = new RpcResult();
        result.setResult(pageInfo);
        return result;
    }

    @RpcMethod("validateContractCode")
    public RpcResult validateContractCode(List<Object> params) {

        RpcResult result = new RpcResult();
        OutputStream out = null;
        InputStream jarIn = null;
        try {
            VerifyUtils.verifyParams(params, 2);
            String contractAddress = (String) params.get(0);
            if (!AddressTool.validAddress(contractAddress)) {
                result.setError(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "contractAddress is inValid."));
                return result;
            }
            // 检查认证状态，未认证的合约继续下一步
            ContractInfo contractInfo = contractService.getContractInfo(contractAddress);
            if (contractInfo == null) {
                result.setError(new RpcResultError(RpcErrorCode.DATA_NOT_EXISTS));
                return result;
            }
            Integer status = contractInfo.getStatus();
            // 已进入以下状态 -> 正在审核 or 通过验证 or 已删除
            if (status > 0) {
                result.setError(new RpcResultError(RpcErrorCode.CONTRACT_VALIDATION_ERROR));
                return result;
            }

            // 生成文件
            String fileDataURL = (String) params.get(1);
            String[] arr = fileDataURL.split(",");
            if (arr.length != 2) {
                result.setError(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "File Data error."));
                return result;
            }
            String headerInfo = arr[0];
            String body = arr[1];
            byte[] fileContent = Base64.getDecoder().decode(body);
            File zipFile = new File(VALIDATE_HOME + contractAddress + ".zip");
            out = new FileOutputStream(zipFile);
            IOUtils.write(fileContent, out);

            boolean isValidationPass = false;
            do {
                // 编译代码
                List<String> resultList = RunShellUtil.run(BASE + File.separator + "bin" + File.separator + "compile.sh", contractAddress);
                if (!resultList.isEmpty()) {
                    String error = resultList.stream().collect(Collectors.joining());
                    Log.error(error);
                    result.setError(new RpcResultError(RpcErrorCode.TX_SHELL_ERROR));
                    break;
                }
                File jarFile = new File(VALIDATE_HOME + contractAddress + File.separator + contractAddress + ".jar");
                jarIn = new FileInputStream(jarFile);
                byte[] validateContractCode = IOUtils.toByteArray(jarIn);

                // 获取智能合约的代码
                String createTxHash = contractInfo.getCreateTxHash();
                Result result1 = NulsSDKTool.getTxWithBytes(createTxHash);
                if (result1.isFailed()) {
                    result.setError(new RpcResultError(RpcErrorCode.DATA_NOT_EXISTS));
                    break;
                }
                CreateContractTransaction tx = (CreateContractTransaction) result1.getData();
                CreateContractData txData = tx.getTxData();
                byte[] contractCode = txData.getCode();

                // 比较代码指令
                isValidationPass = CompareJar.compareJarBytes(contractCode, validateContractCode);
                result.setResult(isValidationPass);

                if(!isValidationPass) {
                    break;
                }

                // 合约认证通过后，更新合约认证状态
                contractInfo.setStatus(2);
                contractInfo.setCertificationTime(System.currentTimeMillis());
                contractService.updateContractInfo(contractInfo);
            } while (false);

            if(!isValidationPass) {
                // 删除上传的文件
                if(zipFile.exists()) {
                    zipFile.delete();
                }
                delFolder(VALIDATE_HOME + contractAddress);
                return new RpcResult().setError(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "Verification failed."));
            }


        } catch (Exception e) {
            Log.error(e);
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, e.getMessage()));
        } finally {
            IOUtils.closeQuietly(jarIn);
            IOUtils.closeQuietly(out);
        }
        return result;
    }

    private void delFolder(String folderPath) {
        try {
            //删除完里面所有内容
            delAllFile(folderPath);
            File myFilePath = new File(folderPath);
            //删除空文件夹
            myFilePath.delete();
        } catch (Exception e) {}
    }

    private boolean delAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delFolder(path + File.separator + tempList[i]);
                flag = true;
            }
        }
        return flag;
    }

    @RpcMethod("getContractCodeTree")
    public RpcResult getContractCodeTree(List<Object> params) {
        RpcResult result = new RpcResult();


        try {
            VerifyUtils.verifyParams(params, 1);
            String contractAddress = (String) params.get(0);
            if (!AddressTool.validAddress(contractAddress)) {
                result.setError(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[contractAddress] is inValid"));
                return result;
            }
            ContractInfo contractInfo = contractService.getContractInfo(contractAddress);
            if (contractInfo == null) {
                result.setError(new RpcResultError(RpcErrorCode.DATA_NOT_EXISTS));
                return result;
            }
            Integer status = contractInfo.getStatus();
            // 检查认证状态，通过认证的合约继续下一步
            if (status != 2) {
                result.setError(new RpcResultError(RpcErrorCode.CONTRACT_NOT_VALIDATION_ERROR));
                return result;
            }

            // 提取文件目录树
            ContractCode root = contractCodeTreeCaches.get(contractAddress);
            if (root == null) {
                result.setError(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "root path is inValid"));
                return result;
            }
            result.setResult(root);
        } catch (Exception e) {
            Log.error(e);
            result.setError(new RpcResultError(RpcErrorCode.PARAMS_ERROR, e.getMessage()));
        }
        return result;
    }

    private LoadingCache<String, ContractCode> contractCodeTreeCaches = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, ContractCode>() {
                @Override
                public ContractCode load(String contractAddress) {
                    return generateContractCodeTree(contractAddress);
                }
            });

    private ContractCode generateContractCodeTree(String contractAddress) {
        File src = new File(VALIDATE_HOME + contractAddress + File.separator + "src");
        ContractCode root = new ContractCode();
        ContractCodeNode rootNode = new ContractCodeNode();
        if (!src.isDirectory()) {
            return null;
        }
        List<ContractCodeNode> children = new ArrayList<>();
        rootNode.setName(src.getName());
        rootNode.setPath(extractFilePath(src));
        rootNode.setDir(true);
        rootNode.setChildren(children);
        root.setRoot(rootNode);
        File[] files = src.listFiles();
        recursive(src.listFiles(), children);
        return root;
    }


    private void recursive(File[] files, List<ContractCodeNode> children) {
        for (File file : files) {
            ContractCodeNode node = new ContractCodeNode();
            children.add(node);
            node.setName(extractFileName(file));
            node.setPath(extractFilePath(file));
            node.setDir(file.isDirectory());
            if (file.isDirectory()) {
                node.setChildren(new ArrayList<>());
                recursive(file.listFiles(), node.getChildren());
            }
        }
    }

    private String extractFileName(File file) {
        if (file.isDirectory()) {
            return file.getName();
        }
        String name = file.getName();
        name = name.replaceAll("\\.java", "");
        return name;
    }

    private String extractFilePath(File file) {
        String path = file.getPath();
        path = path.replaceAll(BASE, "");
        return path;
    }

    @RpcMethod("getContractCode")
    public RpcResult getContractCode(List<Object> params) {
        RpcResult result = new RpcResult();
        try {
            VerifyUtils.verifyParams(params, 2);
            String contractAddress = (String) params.get(0);
            if (!AddressTool.validAddress(contractAddress)) {
                result.setError(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[contractAddress] is inValid"));
                return result;
            }
            // 检查认证状态，通过认证的合约继续下一步
            ContractInfo contractInfo = contractService.getContractInfo(contractAddress);
            if (contractInfo == null) {
                result.setError(new RpcResultError(RpcErrorCode.DATA_NOT_EXISTS));
                return result;
            }
            Integer status = contractInfo.getStatus();
            if (status != 2) {
                result.setError(new RpcResultError(RpcErrorCode.CONTRACT_NOT_VALIDATION_ERROR));
                return result;
            }

            // 提取文件内容
            String filePath = (String) params.get(1);
            String code = contractCodeCaches.get(filePath);
            if(StringUtils.isBlank(code)) {
                result.setError(new RpcResultError(RpcErrorCode.DATA_NOT_EXISTS, "Fail to read contract code."));
                return result;
            }
            result.setResult(code);
        } catch (Exception e) {
            Log.error(e);
            result.setError(new RpcResultError(RpcErrorCode.PARAMS_ERROR, e.getMessage()));
        }
        return result;
    }

    private LoadingCache<String, String> contractCodeCaches = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String filePath) {
                    return readContractCode(filePath);
                }
            });

    private String readContractCode(String filePath) {
        FileInputStream in = null;
        try {
            File file = new File(BASE + filePath);
            if(!file.exists()) {
                return null;
            }
            in = new FileInputStream(file);
            List<String> strings = IOUtils.readLines(in);
            StringBuilder sb = new StringBuilder();
            strings.forEach(a -> {
                sb.append(a).append("\r\n");
            });
            return sb.toString();
        } catch (Exception e) {
            Log.error(e);
            return null;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

}
