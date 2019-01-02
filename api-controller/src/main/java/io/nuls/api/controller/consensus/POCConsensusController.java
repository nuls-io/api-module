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

package io.nuls.api.controller.consensus;

import io.nuls.api.bean.annotation.Controller;
import io.nuls.api.bean.annotation.RpcMethod;
import io.nuls.api.controller.model.RpcResult;
import io.nuls.api.controller.utils.VerifyUtils;

import java.util.List;

/**
 * @author Niels
 */
@Controller
public class POCConsensusController {


    @RpcMethod("getBestRoundHeaderList")
    public RpcResult getBestRoundHeaderList(List<Object> params) {
        VerifyUtils.verifyParams(params, 0);

        //todo
        return null;
    }

    @RpcMethod("getConsensusNodeCount")
    public RpcResult getConsensusNodeCount(List<Object> params) {
        VerifyUtils.verifyParams(params, 0);

        //todo
        return null;
    }

    @RpcMethod("getConsensusStatistical")
    public RpcResult getConsensusStatistical(List<Object> params) {
        VerifyUtils.verifyParams(params, 0);

        //todo
        return null;
    }

    @RpcMethod("getConsensusNodes")
    public RpcResult getConsensusNodes(List<Object> params) {
        VerifyUtils.verifyParams(params, 0);

        //todo
        return null;
    }

    @RpcMethod("getConsensusNode")
    public RpcResult getConsensusNode(List<Object> params) {
        VerifyUtils.verifyParams(params, 0);

        //todo
        return null;
    }

    @RpcMethod("getConsensusNodeStatistical")
    public RpcResult getConsensusNodeStatistical(List<Object> params) {
        VerifyUtils.verifyParams(params, 0);

        //todo
        return null;
    }

    @RpcMethod("getConsensusPublish")
    public RpcResult getConsensusPublish(List<Object> params) {
        VerifyUtils.verifyParams(params, 0);

        //todo
        return null;
    }

    @RpcMethod("getConsensusDeposit")
    public RpcResult getConsensusDeposit(List<Object> params) {
        VerifyUtils.verifyParams(params, 0);

        //todo
        return null;
    }

    @RpcMethod("getBestRoundInfo")
    public RpcResult getBestRoundInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 0);

        //todo
        return null;
    }

    @RpcMethod("getRoundList")
    public RpcResult getRoundList(List<Object> params) {
        VerifyUtils.verifyParams(params, 0);

        //todo
        return null;
    }

}
