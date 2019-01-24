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
import io.nuls.contract.vm.code.FieldCode;
import io.nuls.contract.vm.code.MethodCode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author: PierreLuo
 * @date: 2018/12/26
 */
public class CompareClass {
    public static boolean compareClassCode(ClassCode contractClassCode, ClassCode validateClassCode) {
        if(!fieldValidation(contractClassCode, validateClassCode)) {
            return false;
        }
        if(!methodValidation(contractClassCode, validateClassCode)) {
            return false;
        }
        return true;
    }

    private static boolean fieldValidation(ClassCode contractClassCode, ClassCode validateClassCode) {
        LinkedHashMap<String, FieldCode> contractClassFields = LoadClassFields.loadClassFields(contractClassCode);
        LinkedHashMap<String, FieldCode> validateClassFields = LoadClassFields.loadClassFields(validateClassCode);
        if(!CommonUtil.baseCompareMap(contractClassFields, validateClassFields)) {
            return false;
        }
        FieldCode contractFieldCode;
        Set<Map.Entry<String, FieldCode>> validateEntries = validateClassFields.entrySet();
        for(Map.Entry<String, FieldCode> validateFiledCodeEntry : validateEntries) {
            String validateFieldName = validateFiledCodeEntry.getKey();
            FieldCode validateFieldCode = validateFiledCodeEntry.getValue();
            if((contractFieldCode = contractClassFields.get(validateFieldName)) == null) {
                return false;
            }
            if(!CompareFields.compareFiled(contractFieldCode, validateFieldCode)) {
                return false;
            }
        }
        return true;
    }

    private static boolean methodValidation(ClassCode contractClassCode, ClassCode validateClassCode) {
        LinkedHashMap<String, MethodCode> contractClassMethodMap = LoadClassMethods.loadClassMethods(contractClassCode);
        LinkedHashMap<String, MethodCode> validateClassMethodMap = LoadClassMethods.loadClassMethods(validateClassCode);
        if(!CommonUtil.baseCompareMap(contractClassMethodMap, validateClassMethodMap)) {
            return false;
        }
        MethodCode contractMethodCode;
        Set<Map.Entry<String, MethodCode>> validateEntries = validateClassMethodMap.entrySet();
        for(Map.Entry<String, MethodCode> validateMethodCodeEntry : validateEntries) {
            String validateMethodName = validateMethodCodeEntry.getKey();
            MethodCode validateMethodCode = validateMethodCodeEntry.getValue();
            if((contractMethodCode = contractClassMethodMap.get(validateMethodName)) == null) {
                return false;
            }
            if(!CompareMethods.compareMethod(contractMethodCode, validateMethodCode)) {
                return false;
            }
        }
        return true;
    }
}
