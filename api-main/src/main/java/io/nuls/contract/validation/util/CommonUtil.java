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
package io.nuls.contract.validation.util;

import io.nuls.contract.validation.service.ObjectEqual;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2018/12/26
 */
public class CommonUtil {

    public static boolean baseCompareMap(Map map1, Map map2) {
        if(map1 == null || map2 == null) {
            return false;
        }
        if(map1.size() != map2.size()) {
            return false;
        }
        return true;
    }


    public static boolean isEqualList(Collection<?> list1, Collection<?> list2, ObjectEqual objectEqual) {
        if (list1 == list2) {
            return true;
        } else if (list1 != null && list2 != null && list1.size() == list2.size()) {
            Iterator<?> it1 = list1.iterator();
            Iterator<?> it2 = list2.iterator();
            Object obj1 = null;
            Object obj2 = null;

            while(true) {
                if (it1.hasNext() && it2.hasNext()) {
                    obj1 = it1.next();
                    obj2 = it2.next();
                    if (obj1 == null) {
                        if (obj2 == null) {
                            continue;
                        }
                    } else if (objectEqual.customizeEquals(obj1, obj2)) {
                        continue;
                    }

                    return false;
                }

                return !it1.hasNext() && !it2.hasNext();
            }
        } else {
            return false;
        }
    }

    public static boolean equalsCollectionArray(Collection<?>[] a, Collection<?>[] a2, ObjectEqual objectEqual) {
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        for (int i=0; i<length; i++) {
            Collection<?> o1 = a[i];
            Collection<?> o2 = a2[i];
            if(!isEqualList(o1, o2, objectEqual))
                return false;
        }

        return true;
    }
}
