/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.openjdk.jextract.test.api;

import org.openjdk.jextract.Declaration;
import org.testng.annotations.Test;
import testlib.JextractApiTestBase;

import static org.testng.Assert.assertEquals;

import java.lang.foreign.GroupLayout;

public class TestPackedStructs extends JextractApiTestBase {

    static final String[] NAMES = {
            "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8"
    };

    @Test
    public void testPackedStructs() {
        Declaration.Scoped d = parse("packedstructs.h");
        System.out.println(d);
        for (String name : NAMES) {
            Declaration.Scoped scoped = checkStruct(d, name, "first", "second");
        }
    }
}
