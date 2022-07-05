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
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class CXString {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_POINTER$LAYOUT.withName("data"),
        Constants$root.C_INT$LAYOUT.withName("private_flags"),
        MemoryLayout.paddingLayout(32)
    );
    public static MemoryLayout $LAYOUT() {
        return CXString.$struct$LAYOUT;
    }
    static final VarHandle data$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("data"));
    public static VarHandle data$VH() {
        return CXString.data$VH;
    }
    public static MemorySegment data$get(MemorySegment seg) {
        return (java.lang.foreign.MemorySegment)CXString.data$VH.get(seg);
    }
    public static void data$set( MemorySegment seg, MemorySegment x) {
        CXString.data$VH.set(seg, x);
    }
    public static MemorySegment data$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemorySegment)CXString.data$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void data$set(MemorySegment seg, long index, MemorySegment x) {
        CXString.data$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle private_flags$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("private_flags"));
    public static VarHandle private_flags$VH() {
        return CXString.private_flags$VH;
    }
    public static int private_flags$get(MemorySegment seg) {
        return (int)CXString.private_flags$VH.get(seg);
    }
    public static void private_flags$set( MemorySegment seg, int x) {
        CXString.private_flags$VH.set(seg, x);
    }
    public static int private_flags$get(MemorySegment seg, long index) {
        return (int)CXString.private_flags$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void private_flags$set(MemorySegment seg, long index, int x) {
        CXString.private_flags$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemorySegment addr, Arena session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


