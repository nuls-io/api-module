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
import io.nuls.contract.validation.util.CommonUtil;
import io.nuls.contract.vm.code.MethodCode;

import java.util.LinkedList;

/**
 * @author: PierreLuo
 * @date: 2018/12/26
 */
public class CompareMethods {
    public static boolean compareMethod(MethodCode contractMethodCode, MethodCode validateMethodCode) {
        if(!methodBaseValidation(contractMethodCode, validateMethodCode)) {
            return false;
        }
        if(!methodInstructionsValidation(contractMethodCode, validateMethodCode)) {
            return false;
        }

        return true;
    }

    private static boolean methodBaseValidation(MethodCode contractMethodCode, MethodCode validateMethodCode) {
        if(!contractMethodCode.customizeEquals(validateMethodCode)) {
            return false;
        }
        return true;
    }

    private static boolean methodInstructionsValidation(MethodCode contractMethodCode, MethodCode validateMethodCode) {
        LinkedList<OpCodeDesc> contractMethodInstructions = LoadMethodInstructions.loadMethodInstructions(contractMethodCode);
        LinkedList<OpCodeDesc> validateMethodInstructions = LoadMethodInstructions.loadMethodInstructions(validateMethodCode);
        if(!CompareMethodInstructions.compareMethodInstructions(contractMethodInstructions, validateMethodInstructions)) {
            return false;
        }
        return true;
    }
}
