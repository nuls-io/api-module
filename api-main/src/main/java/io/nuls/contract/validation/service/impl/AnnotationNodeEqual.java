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
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ParameterNode;

/**
 * @author: PierreLuo
 * @date: 2018/12/26
 */
public class AnnotationNodeEqual implements ObjectEqual<AnnotationNode> {

    private static final AnnotationNodeEqual ANNOTATION_NODE_EQUAL = new AnnotationNodeEqual();

    private AnnotationNodeEqual() {
    }

    public static AnnotationNodeEqual getInstance() {
        return ANNOTATION_NODE_EQUAL;
    }

    @Override
    public boolean customizeEquals(AnnotationNode thisO, AnnotationNode o) {
        if (thisO == o) return true;
        if (!(o instanceof AnnotationNode)) return false;

        AnnotationNode that = (AnnotationNode) o;

        if (thisO.desc != null ? !thisO.desc.equals(that.desc) : that.desc != null) return false;
        if (thisO.values != null || that.values != null) return false;

        return true;
    }

}
