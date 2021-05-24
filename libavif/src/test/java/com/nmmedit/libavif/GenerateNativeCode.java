package com.nmmedit.libavif;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.objectweb.asm.Opcodes.ASM5;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class GenerateNativeCode {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void generateNativeRegisterCode() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/libavif/AvifImage.class");
        ClassReader reader = new ClassReader(inputStream);
        reader.accept(new ClassVisitor(ASM5) {
            private String fullName;

            private String code = "/*\n" +
                    "    This file was generated automatically \n" +
                    "*/\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "\n";

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                fullName = "Java_" + name.replace('/', '_');

                code += "#include <jni.h>\n";
                code += String.format("#include \"%s.h\"\n\n\n", name.replace('/', '_'));
                code += "#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))\n" +
                        "\n";
                code += String.format("#define CLASS_NAME \"%s\"\n\n\n", name);
                code += "static const JNINativeMethod methods[]={\n";
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if ((access & Opcodes.ACC_NATIVE) == Opcodes.ACC_NATIVE) {
                    code += String.format("    {\"%s\",\"%s\",(void *) %s_%s},\n\n", name, descriptor, fullName, name);
                }

                return null;
            }

            @Override
            public void visitEnd() {
                code += "};\n" +
                        "\n" +
                        "\n";

                code += "static jboolean registerNativeMethods(JNIEnv* env) {\n" +
                        "    jclass clazz = (*env)->FindClass(env, CLASS_NAME);\n" +
                        "    if (clazz == NULL) {\n" +
                        "        return JNI_FALSE;\n" +
                        "    }\n" +
                        "    if ((*env)->RegisterNatives(env, clazz, methods, NELEM(methods)) < 0) {\n" +
                        "        return JNI_FALSE;\n" +
                        "    }\n" +
                        "\n" +
                        "    return JNI_TRUE;\n" +
                        "}\n" +
                        "\n";

                //JNI_Onload
                code += "JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {\n" +
                        "    JNIEnv* env = NULL;\n" +
                        "\n" +
                        "    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6) != JNI_OK) {\n" +
                        "        return -1;\n" +
                        "    }\n" +
                        "    if (!registerNativeMethods(env)) {\n" +
                        "        return -1;\n" +
                        "    }\n" +
                        "\n" +
                        "    return JNI_VERSION_1_6;\n" +
                        "}"
                ;

                System.out.println(code);
            }
        }, ClassReader.SKIP_CODE/*跳过代码解析*/);

    }
}