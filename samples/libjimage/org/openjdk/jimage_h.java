// Generated by jextract

package org.openjdk;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class jimage_h  {

    /* package-private */ jimage_h() {}
    public static OfByte C_CHAR = Constants$root.C_CHAR$LAYOUT;
    public static OfShort C_SHORT = Constants$root.C_SHORT$LAYOUT;
    public static OfInt C_INT = Constants$root.C_INT$LAYOUT;
    public static OfLong C_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfLong C_LONG_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfFloat C_FLOAT = Constants$root.C_FLOAT$LAYOUT;
    public static OfDouble C_DOUBLE = Constants$root.C_DOUBLE$LAYOUT;
    public static OfAddress C_POINTER = Constants$root.C_POINTER$LAYOUT;
    public static int JIMAGE_MAX_PATH() {
        return (int)4096L;
    }
    public static OfLong jlong = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfInt jint = Constants$root.C_INT$LAYOUT;
    public static OfLong JImageLocationRef = Constants$root.C_LONG_LONG$LAYOUT;
    public static MethodHandle JIMAGE_Open$MH() {
        return RuntimeHelper.requireNonNull(constants$0.JIMAGE_Open$MH,"JIMAGE_Open");
    }
    public static MemorySegment JIMAGE_Open ( MemorySegment name,  MemorySegment error) {
        var mh$ = JIMAGE_Open$MH();
        try {
            return (java.lang.foreign.MemorySegment)mh$.invokeExact(name, error);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle JIMAGE_Close$MH() {
        return RuntimeHelper.requireNonNull(constants$0.JIMAGE_Close$MH,"JIMAGE_Close");
    }
    public static void JIMAGE_Close ( MemorySegment jimage) {
        var mh$ = JIMAGE_Close$MH();
        try {
            mh$.invokeExact(jimage);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle JIMAGE_PackageToModule$MH() {
        return RuntimeHelper.requireNonNull(constants$1.JIMAGE_PackageToModule$MH,"JIMAGE_PackageToModule");
    }
    public static MemorySegment JIMAGE_PackageToModule ( MemorySegment jimage,  MemorySegment package_name) {
        var mh$ = JIMAGE_PackageToModule$MH();
        try {
            return (java.lang.foreign.MemorySegment)mh$.invokeExact(jimage, package_name);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle JIMAGE_FindResource$MH() {
        return RuntimeHelper.requireNonNull(constants$1.JIMAGE_FindResource$MH,"JIMAGE_FindResource");
    }
    public static long JIMAGE_FindResource ( MemorySegment jimage,  MemorySegment module_name,  MemorySegment version,  MemorySegment name,  MemorySegment size) {
        var mh$ = JIMAGE_FindResource$MH();
        try {
            return (long)mh$.invokeExact(jimage, module_name, version, name, size);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle JIMAGE_GetResource$MH() {
        return RuntimeHelper.requireNonNull(constants$2.JIMAGE_GetResource$MH,"JIMAGE_GetResource");
    }
    public static long JIMAGE_GetResource ( MemorySegment jimage,  long location,  MemorySegment buffer,  long size) {
        var mh$ = JIMAGE_GetResource$MH();
        try {
            return (long)mh$.invokeExact(jimage, location, buffer, size);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle JIMAGE_ResourceIterator$MH() {
        return RuntimeHelper.requireNonNull(constants$2.JIMAGE_ResourceIterator$MH,"JIMAGE_ResourceIterator");
    }
    public static void JIMAGE_ResourceIterator ( MemorySegment jimage,  MemorySegment visitor,  MemorySegment arg) {
        var mh$ = JIMAGE_ResourceIterator$MH();
        try {
            mh$.invokeExact(jimage, visitor, arg);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static int JIMAGE_NOT_FOUND() {
        return (int)0L;
    }
    public static int JIMAGE_BAD_MAGIC() {
        return (int)-1L;
    }
    public static int JIMAGE_BAD_VERSION() {
        return (int)-2L;
    }
    public static int JIMAGE_CORRUPTED() {
        return (int)-3L;
    }
}


