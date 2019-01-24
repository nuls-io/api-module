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
package io.nuls.contract.validation.service.impl;

import io.nuls.contract.validation.service.ObjectEqual;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;

/**
 * @author: PierreLuo
 * @date: 2018/12/26
 */
public class LocalVariableAnnotationNodeEqual implements ObjectEqual<LocalVariableAnnotationNode> {

    private static final LocalVariableAnnotationNodeEqual LOCAL_VARIABLE_ANNOTATION_NODE_EQUAL = new LocalVariableAnnotationNodeEqual();

    private LocalVariableAnnotationNodeEqual() {
    }

    public static LocalVariableAnnotationNodeEqual getInstance() {
        return LOCAL_VARIABLE_ANNOTATION_NODE_EQUAL;
    }

    @Override
    public boolean customizeEquals(LocalVariableAnnotationNode thisO, LocalVariableAnnotationNode o) {
        if (thisO == o) return true;
        if (!(o instanceof LocalVariableAnnotationNode)) return false;

        LocalVariableAnnotationNode that = (LocalVariableAnnotationNode) o;

        if (thisO.typeRef != that.typeRef) return false;
        if (thisO.typePath != null ? !typePathEqual(thisO.typePath, that.typePath) : that.typePath != null) return false;

        if (thisO.desc != null ? !thisO.desc.equals(that.desc) : that.desc != null) return false;
        if (thisO.values != null || that.values != null) return false;

        return true;
    }

    private boolean typePathEqual(TypePath typePath, TypePath that) {
        if(that == null) return false;
        if(typePath.getLength() != that.getLength()) return false;
        String thisStr = typePath.toString();
        String thatStr = that.toString();
        if (thisStr != null ? !thisStr.equals(thatStr) : thisStr != null) return false;

        return true;
    }

}
