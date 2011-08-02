/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 */
package org.fusesource.leveldbjni;

import org.fusesource.hawtjni.runtime.*;

import java.security.Key;

import static org.fusesource.hawtjni.runtime.FieldFlag.CONSTANT;
import static org.fusesource.hawtjni.runtime.FieldFlag.POINTER_FIELD;
import static org.fusesource.hawtjni.runtime.MethodFlag.CONSTANT_INITIALIZER;
import static org.fusesource.hawtjni.runtime.MethodFlag.JNI;
import static org.fusesource.hawtjni.runtime.MethodFlag.POINTER_RETURN;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
abstract class Comparator extends NativeObject {

    @JniClass(name="JNIComparator", flags={ClassFlag.STRUCT, ClassFlag.CPP})
    static public class ComparatorJNI {

        static {
            DB.LIBRARY.load();
            init();
        }

        @JniMethod(flags={MethodFlag.CPP_NEW}, cast="JNIComparator *")
        public static final native long create();
        @JniMethod(flags={MethodFlag.CPP_DELETE})
        public static final native void delete(@JniArg(cast="JNIComparator *") long ptr);

        public static final native void memmove (
                @JniArg(cast="void *") long dest,
                @JniArg(cast="const void *", flags={ArgFlag.NO_OUT, ArgFlag.CRITICAL}) ComparatorJNI src,
                @JniArg(cast="size_t") long size);

        public static final native void memmove (
                @JniArg(cast="void *", flags={ArgFlag.NO_IN, ArgFlag.CRITICAL}) ComparatorJNI dest,
                @JniArg(cast="const void *") long src,
                @JniArg(cast="size_t") long size);

        @JniField(cast="JNIEnv *")
        long env;

        @JniField(cast="jobject", flags={POINTER_FIELD})
        long target;

        @JniField(cast="jmethodID", flags={POINTER_FIELD})
        long compare_method;

        @JniField(cast="const char *")
        long name;

        @JniMethod(flags={CONSTANT_INITIALIZER})
        private static final native void init();

        @JniField(flags={CONSTANT}, accessor="sizeof(struct JNIComparator)")
        static int SIZEOF;

        @JniField(flags={CONSTANT}, cast="const Comparator*", accessor="leveldb::BytewiseComparator()")
        private static long BYTEWISE_COMPARATOR;

        @JniField(flags={CONSTANT}, cast="JNIEnv *", accessor="env")
        static long ENV;

        @JniMethod(flags={JNI, POINTER_RETURN}, cast="jobject")
        public static final native long NewGlobalRef(
                Object target);

        @JniMethod(flags={JNI}, cast="jobject")
        public static final native void DeleteGlobalRef(
                @JniArg(cast="jobject", flags={ArgFlag.POINTER_ARG})
                long target);

        @JniMethod(flags={JNI, POINTER_RETURN}, cast="jclass")
        public static final native long GetObjectClass(
                Object target);

        @JniMethod(flags={JNI, POINTER_RETURN}, cast="jmethodID")
        public static final native long GetMethodID(
                @JniArg(cast="jclass", flags={ArgFlag.POINTER_ARG})
                long clazz,
                String name,
                String signature);


    }

    private NativeBuffer name_buffer;
    private long globalRef;

    public Comparator() {
        super(ComparatorJNI.create());
        try {
            name_buffer = new NativeBuffer(name());
            globalRef = ComparatorJNI.NewGlobalRef(this);
            if( globalRef==0 ) {
                throw new RuntimeException("jni call failed: NewGlobalRef");
            }
            long clz = ComparatorJNI.GetObjectClass(this);
            if( clz==0 ) {
                throw new RuntimeException("jni call failed: GetObjectClass");
            }

            ComparatorJNI struct = new ComparatorJNI();
            struct.compare_method = ComparatorJNI.GetMethodID(clz, "compare", "(JJ)I");
            if( struct.compare_method==0 ) {
                throw new RuntimeException("jni call failed: GetMethodID");
            }
            struct.env = ComparatorJNI.ENV;
            struct.target = globalRef;
            struct.name = name_buffer.pointer();
            ComparatorJNI.memmove(self, struct, ComparatorJNI.SIZEOF);

        } catch (RuntimeException e) {
            delete();
            throw e;
        }
    }

    public static final Comparator BYTEWISE_COMPARATOR = new Comparator(ComparatorJNI.BYTEWISE_COMPARATOR) {
        @Override
        public void delete() {
            // we won't really delete this one since it's static.
        }
        @Override
        int compare(byte[] key1, byte[] key2) {
            throw new UnsupportedOperationException();
        }
        @Override
        String name() {
            throw new UnsupportedOperationException();
        }
    };

    Comparator(long ptr) {
        super(ptr);
    }

    public void delete() {
        if( name_buffer!=null ) {
            name_buffer.delete();
            name_buffer = null;
        }
        if( globalRef!=0 ) {
            ComparatorJNI.DeleteGlobalRef(globalRef);
            globalRef = 0;
        }
    }

    private int compare(long ptr1, long ptr2) {
        Slice s1 = new Slice();
        s1.read(ptr1, 0);
        Slice s2 = new Slice();
        s2.read(ptr2, 0);
        return compare(s1.toByteArray(), s2.toByteArray());
    }

    abstract int compare(byte[] key1, byte[] key2);
    abstract String name();

}