// Generated by jextract

package org.unix;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$0 {

    static final FunctionDescriptor dlopen$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle dlopen$MH = RuntimeHelper.downcallHandle(
        "dlopen",
        constants$0.dlopen$FUNC, false
    );
    static final FunctionDescriptor dlclose$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle dlclose$MH = RuntimeHelper.downcallHandle(
        "dlclose",
        constants$0.dlclose$FUNC, false
    );
    static final FunctionDescriptor dlsym$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle dlsym$MH = RuntimeHelper.downcallHandle(
        "dlsym",
        constants$0.dlsym$FUNC, false
    );
    static final FunctionDescriptor dlerror$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT);
    static final MethodHandle dlerror$MH = RuntimeHelper.downcallHandle(
        "dlerror",
        constants$0.dlerror$FUNC, false
    );
}


