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

import io.nuls.contract.validation.util.CommonUtil;
import io.nuls.contract.vm.code.ClassCode;
import io.nuls.contract.vm.code.ClassCodeLoader;

import java.util.Map;
import java.util.Set;

/**
 * @author: PierreLuo
 * @date: 2018/12/26
 */
public class CompareJar {

    public static boolean compareJarBytes(byte[] contractCode, byte[] validateCode) {
        Map<String, ClassCode> contractCodeMap = ClassCodeLoader.loadJarCache(contractCode);
        Map<String, ClassCode> validateCodeMap = ClassCodeLoader.loadJarCache(validateCode);
        if(!CommonUtil.baseCompareMap(contractCodeMap, validateCodeMap)) {
            return false;
        }
        ClassCode contractClassCode;
        Set<Map.Entry<String, ClassCode>> validateEntries = validateCodeMap.entrySet();
        for(Map.Entry<String, ClassCode> validateClassCodeEntry : validateEntries) {
            String validateClassName = validateClassCodeEntry.getKey();
            ClassCode validateClassCode = validateClassCodeEntry.getValue();
            if((contractClassCode = contractCodeMap.get(validateClassName)) == null) {
                return false;
            }
            if(!CompareClass.compareClassCode(contractClassCode, validateClassCode)) {
                return false;
            }
        }
        return true;
    }
}
