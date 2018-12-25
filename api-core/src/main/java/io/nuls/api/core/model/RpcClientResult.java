/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.api.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.sdk.core.contast.ErrorCode;
import io.nuls.sdk.core.contast.KernelErrorCode;
import io.nuls.sdk.core.model.ErrorData;
import io.nuls.sdk.core.model.Result;


import java.util.Map;

/**
 * @author Niels
 * @date 2017/10/31
 */
public class RpcClientResult<T> {
    private boolean success;
    private String code;
    private String msg;
    private T data;

    public RpcClientResult() {
    }

    public RpcClientResult(boolean success, String code, String msg) {
        this.success = success;
        this.code = code;
        this.msg = msg;
    }

    public RpcClientResult(boolean success, String code, String msg, T data) {
        this.success = success;
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public RpcClientResult(boolean success, ErrorCode errorCode) {
        this.success = success;
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return null;
        }

    }

    public static RpcClientResult getSuccess() {
        return new RpcClientResult(true, KernelErrorCode.SUCCESS);
    }

    public static RpcClientResult getFailed() {
        return new RpcClientResult(false, KernelErrorCode.FAILED);
    }

    public static RpcClientResult getFailed(String msg) {
        return new RpcClientResult(false, KernelErrorCode.FAILED.getCode(), msg);
    }

    public static RpcClientResult getFailed(ErrorCode errorCode) {
        return new RpcClientResult(false, errorCode);
    }

    public boolean isSuccess() {
        return success;
    }

    @JsonIgnore
    public boolean isFailed() {
        if (success) {
            return false;
        }
        if (data != null) {
            Map<String, Object> errorMap = (Map<String, Object>) data;
            code = (String) errorMap.get("code");
            msg = (String) errorMap.get("msg");
            data = null;
        }
        return true;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public static RpcClientResult errorResult(Result result) {
        RpcClientResult clientResult = new RpcClientResult();
        clientResult.setSuccess(false);
        ErrorData errorData = (ErrorData) result.getData();
        clientResult.setCode(errorData.getCode());
        clientResult.setMsg(errorData.getMsg());
        return clientResult;
    }
}
