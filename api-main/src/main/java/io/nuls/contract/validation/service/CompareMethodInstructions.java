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
package io.nuls.contract.validation.service;

import io.nuls.contract.validation.model.OpCodeDesc;
import io.nuls.contract.vm.OpCode;

import java.util.LinkedList;

/**
 * @author: PierreLuo
 * @date: 2018/12/26
 */
public class CompareMethodInstructions {
    public static boolean compareMethodInstructions(LinkedList<OpCodeDesc> contractMethodInstructions, LinkedList<OpCodeDesc> validateMethodInstructions) {
        int contractSize = contractMethodInstructions.size();
        int validateSize = validateMethodInstructions.size();
        int loopLength = Math.min(contractSize, validateSize);
        int failedIndex = -1;
        for (int i = 0; i < loopLength; i++) {
            if(!contractMethodInstructions.get(i).equals(validateMethodInstructions.get(i))) {
                failedIndex = i;
                break;
            }
        }
        if(failedIndex != -1) {
            // 解析 LDC, GETFIELD 指令，逆向分析
            //TODO 解析 INVOKEVIRTUAL(? -> 是否需要解析固定类型才作兼容判断 -> "java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;")
            OpCodeDesc contractOpCodeDesc = contractMethodInstructions.get(failedIndex);
            OpCodeDesc validateOpCodeDesc = validateMethodInstructions.get(failedIndex);
            if(!OpCode.LDC.toString().equals(contractOpCodeDesc.getOpCode())
                || !OpCode.LDC.toString().equals(validateOpCodeDesc.getOpCode())) {
                return false;
            }
            String contractStr = contractOpCodeDesc.getDesc();
            String validateStr = validateOpCodeDesc.getDesc();
            OpCodeDesc opCodeDesc;
            String opCode;
            String desc;
            for(int i = failedIndex + 1; i < contractSize; i++) {
                opCodeDesc = contractMethodInstructions.get(i);
                opCode = opCodeDesc.getOpCode();
                if(OpCode.LDC.toString().equals(opCode) || OpCode.GETFIELD.toString().equals(opCode)) {
                    desc = opCodeDesc.getDesc();
                    contractStr += desc;
                }
            }

            for(int i = failedIndex + 1; i < validateSize; i++) {
                opCodeDesc = validateMethodInstructions.get(i);
                opCode = opCodeDesc.getOpCode();
                if(OpCode.LDC.toString().equals(opCode) || OpCode.GETFIELD.toString().equals(opCode)) {
                    desc = opCodeDesc.getDesc();
                    validateStr += desc;
                }
            }

            if(!contractStr.equals(validateStr)) {
                return false;
            }

        }
        return true;
    }
}
