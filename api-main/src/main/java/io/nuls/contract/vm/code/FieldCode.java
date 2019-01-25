/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.contract.vm.code;

import io.nuls.contract.validation.service.impl.AnnotationNodeEqual;
import io.nuls.contract.validation.service.impl.AttributeEqual;
import io.nuls.contract.validation.service.impl.TypeAnnotationNodeEqual;
import io.nuls.contract.validation.util.CommonUtil;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.collections4.ListUtils;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.List;

@EqualsAndHashCode
@ToString
public class FieldCode {

    /**
     * The field's access flags (see {@link org.objectweb.asm.Opcodes}). This field also indicates if
     * the field is synthetic and/or deprecated.
     */
    public final int access;

    /**
     * The field's name.
     */
    public final String name;

    /**
     * The field's descriptor (see {@link org.objectweb.asm.Type}).
     */
    public final String desc;

    /**
     * The field's signature. May be <tt>null</tt>.
     */
    public final String signature;

    /**
     * The field's initial value. This field, which may be <tt>null</tt> if the field does not have an
     * initial value, must be an {@link Integer}, a {@link Float}, a {@link Long}, a {@link Double} or
     * a {@link String}.
     */
    public final Object value;

    /**
     * The runtime visible annotations of this field. May be <tt>null</tt>.
     */
    public final List<AnnotationNode> visibleAnnotations;

    /**
     * The runtime invisible annotations of this field. May be <tt>null</tt>.
     */
    public final List<AnnotationNode> invisibleAnnotations;

    /**
     * The runtime visible type annotations of this field. May be <tt>null</tt>.
     */
    public final List<TypeAnnotationNode> visibleTypeAnnotations;

    /**
     * The runtime invisible type annotations of this field. May be <tt>null</tt>.
     */
    public final List<TypeAnnotationNode> invisibleTypeAnnotations;

    /**
     * The non standard attributes of this field. * May be <tt>null</tt>.
     */
    public final List<Attribute> attrs;

    public final VariableType variableType;

    public final boolean isStatic;

    public final boolean isFinal;

    public final boolean isSynthetic;

    public FieldCode(FieldNode fieldNode) {
        access = fieldNode.access;
        name = fieldNode.name;
        desc = fieldNode.desc;
        signature = fieldNode.signature;
        value = fieldNode.value;
        visibleAnnotations = ListUtils.emptyIfNull(fieldNode.visibleAnnotations);
        invisibleAnnotations = ListUtils.emptyIfNull(fieldNode.invisibleAnnotations);
        visibleTypeAnnotations = ListUtils.emptyIfNull(fieldNode.visibleTypeAnnotations);
        invisibleTypeAnnotations = ListUtils.emptyIfNull(fieldNode.invisibleTypeAnnotations);
        attrs = ListUtils.emptyIfNull(fieldNode.attrs);
        //
        variableType = VariableType.valueOf(desc);
        isStatic = (access & Opcodes.ACC_STATIC) != 0;
        isFinal = (access & Opcodes.ACC_FINAL) != 0;
        isSynthetic = (access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    public boolean customizeEquals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldCode)) return false;

        FieldCode fieldCode = (FieldCode) o;

        if (access != fieldCode.access) return false;
        if (isStatic != fieldCode.isStatic) return false;
        if (isFinal != fieldCode.isFinal) return false;
        if (isSynthetic != fieldCode.isSynthetic) return false;
        if (name != null ? !name.equals(fieldCode.name) : fieldCode.name != null) return false;
        if (desc != null ? !desc.equals(fieldCode.desc) : fieldCode.desc != null) return false;
        if (signature != null ? !signature.equals(fieldCode.signature) : fieldCode.signature != null) return false;
        if (value != null ? !value.equals(fieldCode.value) : fieldCode.value != null) return false;
        if (!CommonUtil.isEqualList(visibleAnnotations, fieldCode.visibleAnnotations, AnnotationNodeEqual.getInstance())) return false;
        if (!CommonUtil.isEqualList(invisibleAnnotations, fieldCode.invisibleAnnotations, AnnotationNodeEqual.getInstance())) return false;
        if (!CommonUtil.isEqualList(visibleTypeAnnotations, fieldCode.visibleTypeAnnotations, TypeAnnotationNodeEqual.getInstance())) return false;
        if (!CommonUtil.isEqualList(invisibleTypeAnnotations, fieldCode.invisibleTypeAnnotations, TypeAnnotationNodeEqual.getInstance())) return false;
        if (!CommonUtil.isEqualList(attrs, fieldCode.attrs, AttributeEqual.getInstance())) return false;
        if (variableType != null ? !variableType.equals(fieldCode.variableType) : fieldCode.variableType != null)
            return false;

        return true;
    }

}
