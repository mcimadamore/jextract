/*
 *  Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

// Generated by jextract

package org.openjdk.jextract.clang.libclang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.ValueLayout.*;
class constants$11 {

    static final FunctionDescriptor clang_getArgType$FUNC = FunctionDescriptor.of(MemoryLayout.structLayout(
        Constants$root.C_INT$LAYOUT.withName("kind"),
        MemoryLayout.paddingLayout(32),
        MemoryLayout.sequenceLayout(2, Constants$root.C_POINTER$LAYOUT).withName("data")
    ),
        MemoryLayout.structLayout(
            Constants$root.C_INT$LAYOUT.withName("kind"),
            MemoryLayout.paddingLayout(32),
            MemoryLayout.sequenceLayout(2, Constants$root.C_POINTER$LAYOUT).withName("data")
        ),
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle clang_getArgType$MH = RuntimeHelper.downcallHandle(
        "clang_getArgType",
        constants$11.clang_getArgType$FUNC, false
    );
    static final FunctionDescriptor clang_isFunctionTypeVariadic$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        MemoryLayout.structLayout(
            Constants$root.C_INT$LAYOUT.withName("kind"),
            MemoryLayout.paddingLayout(32),
            MemoryLayout.sequenceLayout(2, Constants$root.C_POINTER$LAYOUT).withName("data")
        )
    );
    static final MethodHandle clang_isFunctionTypeVariadic$MH = RuntimeHelper.downcallHandle(
        "clang_isFunctionTypeVariadic",
        constants$11.clang_isFunctionTypeVariadic$FUNC, false
    );
    static final FunctionDescriptor clang_getCursorResultType$FUNC = FunctionDescriptor.of(MemoryLayout.structLayout(
        Constants$root.C_INT$LAYOUT.withName("kind"),
        MemoryLayout.paddingLayout(32),
        MemoryLayout.sequenceLayout(2, Constants$root.C_POINTER$LAYOUT).withName("data")
    ),
        MemoryLayout.structLayout(
            Constants$root.C_INT$LAYOUT.withName("kind"),
            Constants$root.C_INT$LAYOUT.withName("xdata"),
            MemoryLayout.sequenceLayout(3, Constants$root.C_POINTER$LAYOUT).withName("data")
        )
    );
    static final MethodHandle clang_getCursorResultType$MH = RuntimeHelper.downcallHandle(
        "clang_getCursorResultType",
        constants$11.clang_getCursorResultType$FUNC, false
    );
    static final FunctionDescriptor clang_getElementType$FUNC = FunctionDescriptor.of(MemoryLayout.structLayout(
        Constants$root.C_INT$LAYOUT.withName("kind"),
        MemoryLayout.paddingLayout(32),
        MemoryLayout.sequenceLayout(2, Constants$root.C_POINTER$LAYOUT).withName("data")
    ),
        MemoryLayout.structLayout(
            Constants$root.C_INT$LAYOUT.withName("kind"),
            MemoryLayout.paddingLayout(32),
            MemoryLayout.sequenceLayout(2, Constants$root.C_POINTER$LAYOUT).withName("data")
        )
    );
    static final MethodHandle clang_getElementType$MH = RuntimeHelper.downcallHandle(
        "clang_getElementType",
        constants$11.clang_getElementType$FUNC, false
    );
    static final FunctionDescriptor clang_getNumElements$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        MemoryLayout.structLayout(
            Constants$root.C_INT$LAYOUT.withName("kind"),
            MemoryLayout.paddingLayout(32),
            MemoryLayout.sequenceLayout(2, Constants$root.C_POINTER$LAYOUT).withName("data")
        )
    );
    static final MethodHandle clang_getNumElements$MH = RuntimeHelper.downcallHandle(
        "clang_getNumElements",
        constants$11.clang_getNumElements$FUNC, false
    );
    static final FunctionDescriptor clang_getArrayElementType$FUNC = FunctionDescriptor.of(MemoryLayout.structLayout(
        Constants$root.C_INT$LAYOUT.withName("kind"),
        MemoryLayout.paddingLayout(32),
        MemoryLayout.sequenceLayout(2, Constants$root.C_POINTER$LAYOUT).withName("data")
    ),
        MemoryLayout.structLayout(
            Constants$root.C_INT$LAYOUT.withName("kind"),
            MemoryLayout.paddingLayout(32),
            MemoryLayout.sequenceLayout(2, Constants$root.C_POINTER$LAYOUT).withName("data")
        )
    );
    static final MethodHandle clang_getArrayElementType$MH = RuntimeHelper.downcallHandle(
        "clang_getArrayElementType",
        constants$11.clang_getArrayElementType$FUNC, false
    );
}


