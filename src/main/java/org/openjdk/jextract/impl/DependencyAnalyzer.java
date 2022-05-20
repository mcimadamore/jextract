package org.openjdk.jextract.impl;


import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

public class DependencyAnalyzer implements Declaration.Visitor<Void, Boolean> {

    private final IncludeHelper includeHelper;

    DependencyAnalyzer(IncludeHelper includeHelper) {
        this.includeHelper = includeHelper;
    }

    public void augment(Declaration.Scoped toplevel) {
        toplevel.members().forEach(m -> m.accept(this, false));
    }

    @Override
    public Void visitScoped(Declaration.Scoped d, Boolean forceInclude) {
        if (forceInclude || includeHelper.isIncluded(d)) {
            includeHelper.addSymbol(IncludeHelper.IncludeKind.fromScoped(d), d.name());
            d.members().forEach(m -> m.accept(this, true));
        }
        return null;
    }

    @Override
    public Void visitFunction(Declaration.Function d, Boolean forceInclude) {
        if (forceInclude || includeHelper.isIncluded(d)) {
            scanType(d.type());
        }
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable d, Boolean forceInclude) {
        if (forceInclude || includeHelper.isIncluded(d)) {
            scanType(d.type());
        }
        return null;
    }

    @Override
    public Void visitConstant(Declaration.Constant d, Boolean forceInclude) {
        // nothing to do
        return null;
    }

    @Override
    public Void visitTypedef(Declaration.Typedef d, Boolean forceInclude) {
        if (forceInclude || includeHelper.isIncluded(d)) {
            includeHelper.addSymbol(IncludeHelper.IncludeKind.TYPEDEF, d.name());
            scanType(d.type());
        }
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration d, Boolean forceInclude) {
        throw new UnsupportedOperationException();
    }

    class TypeScanner implements Type.Visitor<Void, Void> {
        @Override
        public Void visitPrimitive(Type.Primitive t, Void unused) {
            // do nothing
            return null;
        }

        @Override
        public Void visitFunction(Type.Function t, Void unused) {
            t.argumentTypes().forEach(a -> a.accept(this, null));
            t.returnType().accept(this, null);
            return null;
        }

        @Override
        public Void visitDeclared(Type.Declared t, Void unused) {
            t.tree().accept(DependencyAnalyzer.this, true);
            return null;
        }

        @Override
        public Void visitDelegated(Type.Delegated t, Void unused) {
            if (t.kind() == Type.Delegated.Kind.TYPEDEF) {
                includeHelper.addSymbol(IncludeHelper.IncludeKind.TYPEDEF, t.name().get());
            }
            t.type().accept(this, null);
            return null;
        }

        @Override
        public Void visitArray(Type.Array t, Void unused) {
            t.elementType().accept(this, null);
            return null;
        }

        @Override
        public Void visitType(Type t, Void unused) {
            throw new IllegalStateException("Cannot get here!");
        }
    }

    void scanType(Type t) {
        t.accept(new TypeScanner(), null);
    }

    public static final DependencyAnalyzer STRICT = new DependencyAnalyzer(null) {
        @Override
        public void augment(Declaration.Scoped toplevel) {
            // do nothing
        }
    };

    public static DependencyAnalyzer auto(IncludeHelper includeHelper) {
        return new DependencyAnalyzer(includeHelper);
    }
}
