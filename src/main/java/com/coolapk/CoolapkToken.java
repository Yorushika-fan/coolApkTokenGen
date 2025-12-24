package com.coolapk;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;

/**
 * 酷安 X-App-Token 生成器
 * 使用 unidbg 模拟调用 libauth.so 中的 getToken 函数
 */
public class CoolapkToken extends AbstractJni {
    
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    
    // 包名和版本信息
    private static final String PACKAGE_NAME = "com.coolapk.market";
    private static final int VERSION_CODE = 2512091;
    private static final String VERSION_NAME = "15.9.1";
    
    // 酷安 APK 签名 (从 libauth.so 中提取的硬编码签名)
    private static final String APK_SIGNATURE_HEX = 
        "30820259308201c2a00302010202045044cd17300d06092a864886f70d01010505003071310b300906035504061302434e310f300d06035504080c06e58c97e4baac310f300d06035504070c06e58c97e4baac31143012060355040a130b436f6f6c41706b2e636f6d31143012060355040b130b436f6f6c41706b2e636f6d311430120603550403130b436f6f6c41706b2e636f6d301e170d3132303930333135333033315a170d3430303132303135333033315a3071310b300906035504061302434e310f300d06035504080c06e58c97e4baac310f300d06035504070c06e58c97e4baac31143012060355040a130b436f6f6c41706b2e636f6d31143012060355040b130b436f6f6c41706b2e636f6d311430120603550403130b436f6f6c41706b2e636f6d30819f300d06092a864886f70d010101050003818d0030818902818100b1441c2288e4de72d2c7e81a3ab29e2e63ca3ad271636dfdac60eb9c0d5b4b67ed6be9d236bc49087c1c207b4bdcd1fc6150198fbdf3f882c04c8415d953508ea117cb1eaf3f06fc7f55086dc125ad477ebd7db98fd9769934915b72aaaf1276b1fcd7b5f7f779c3b2ebc4b701781f4d00810bd57ace023c7cab757314184f2d0203010001300d06092a864886f70d01010505000381810066e7f8317544e55b4b606bb00426179d0bdee1d865920abd39bf6273e369b15a53efe96a745d0b53051805d15af7bb8d59b87d5dfc6cb1f0afeecce2d12c8c3612b9c2479188db38a8026092f71ddc1ec67c5b312ea1ff78053901bd0dcf1c2282748a657f110e7dac40575e9547c5d2383de10d618f981b419fbefddec4b240";
    
    private static final byte[] APK_SIGNATURE = hexStringToByteArray(APK_SIGNATURE_HEX);
    
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
    
    public CoolapkToken() throws IOException {
        // 创建模拟器
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName(PACKAGE_NAME)
                .build();
        
        // 获取内存
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        
        // 创建虚拟机
        vm = emulator.createDalvikVM();
        vm.setJni(this);
        vm.setVerbose(true);
        
        // 加载 libauth.so
        File soFile = findSoFile();
        if (soFile == null) {
            throw new IOException("libauth.so not found!");
        }
        
        DalvikModule dm = vm.loadLibrary(soFile, false);
        module = dm.getModule();
        
        System.out.println("[*] libauth.so loaded at: 0x" + Long.toHexString(module.base));
        
        // 调用 JNI_OnLoad
        dm.callJNI_OnLoad(emulator);
        System.out.println("[*] JNI_OnLoad called");
    }
    
    private File findSoFile() {
        String[] paths = {
            "libauth.so",
            "lib/arm64-v8a/libauth.so",
            "../libauth.so",
            "src/main/java/com/coolapk/libauth.so",
            "unidbg-coolapk/src/main/java/com/coolapk/libauth.so"
        };
        
        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                System.out.println("[*] Found libauth.so at: " + file.getAbsolutePath());
                return file;
            }
        }
        return null;
    }
    
    /**
     * 生成 X-App-Token
     */
    public String getToken(String deviceId) {
        DvmClass authUtilsClass = vm.resolveClass("com/coolapk/market/util/AuthUtils");
        DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(null);
        
        StringObject result = authUtilsClass.callStaticJniMethodObject(
                emulator,
                "getToken(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;",
                context,
                new StringObject(vm, deviceId)
        );
        
        return result != null ? result.getValue() : null;
    }
    
    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "android/content/Context->getPackageName()Ljava/lang/String;":
                return new StringObject(vm, PACKAGE_NAME);
                
            case "android/content/Context->getPackageManager()Landroid/content/pm/PackageManager;":
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
                
            case "android/content/pm/PackageManager->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;":
                return vm.resolveClass("android/content/pm/PackageInfo").newObject(null);
                
            case "android/content/pm/SigningInfo->getApkContentsSigners()[Landroid/content/pm/Signature;":
                DvmClass signatureClass = vm.resolveClass("android/content/pm/Signature");
                DvmObject<?> signatureObj = signatureClass.newObject(APK_SIGNATURE);
                return new ArrayObject(signatureObj);
                
            case "android/content/pm/Signature->toCharsString()Ljava/lang/String;":
                return new StringObject(vm, APK_SIGNATURE_HEX);
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }
    
    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if ("android/content/pm/PackageInfo->getVersionCode()I".equals(signature)) {
            return VERSION_CODE;
        }
        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }
    
    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            case "android/content/pm/PackageInfo->versionCode:I":
                return vm.resolveClass("java/lang/Integer").newObject(VERSION_CODE);
                
            case "android/content/pm/PackageInfo->versionName:Ljava/lang/String;":
                return new StringObject(vm, VERSION_NAME);
                
            case "android/content/pm/PackageInfo->signatures:[Landroid/content/pm/Signature;":
                DvmClass signatureClass = vm.resolveClass("android/content/pm/Signature");
                DvmObject<?> signatureObj = signatureClass.newObject(APK_SIGNATURE);
                return new ArrayObject(signatureObj);
                
            case "android/content/pm/PackageInfo->signingInfo:Landroid/content/pm/SigningInfo;":
                return vm.resolveClass("android/content/pm/SigningInfo").newObject(null);
        }
        return super.getObjectField(vm, dvmObject, signature);
    }
    
    @Override
    public int getIntField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        if ("android/content/pm/PackageInfo->versionCode:I".equals(signature)) {
            return VERSION_CODE;
        }
        return super.getIntField(vm, dvmObject, signature);
    }
    
    public void destroy() throws IOException {
        emulator.close();
    }
    
    public static void main(String[] args) throws IOException {
        // 服务器模式
        if (args.length > 0 && "server".equals(args[0])) {
            int port = 8080;
            if (args.length > 1) {
                try {
                    port = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port: " + args[1]);
                    System.exit(1);
                }
            }
            try {
                CoolapkTokenServer.startServer(port);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            return;
        }
        
        // 命令行模式
        String deviceId = "sxWduByOxADMuITM5ADNy4SQzEVQgszREZjTQZENwMjMgsTat9WYphFI7kWbvFWaYByOgsDI7AyOwc2d3gXY1pVMvNFSsZTR5pUZE5mM2oWQvpnc3IkSWh0aEVFR";
        
        if (args.length > 0) {
            deviceId = args[0];
        }
        
        System.out.println("============================================================");
        System.out.println("酷安 X-App-Token 生成器 (unidbg)");
        System.out.println("============================================================");
        System.out.println("\nUsage:");
        System.out.println("  java -jar unidbg-coolapk.jar [deviceId]     - Generate token");
        System.out.println("  java -jar unidbg-coolapk.jar server [port]  - Start API server");
        
        CoolapkToken coolapk = new CoolapkToken();
        
        try {
            System.out.println("\n[*] Device ID: " + deviceId.substring(0, Math.min(30, deviceId.length())) + "...");
            System.out.println("[*] Generating token...\n");
            
            String token = coolapk.getToken(deviceId);
            
            if (token != null) {
                System.out.println("\n============================================================");
                System.out.println("[+] X-App-Token: " + token);
                System.out.println("============================================================");
            } else {
                System.out.println("\n[-] Failed to generate token");
            }
        } finally {
            coolapk.destroy();
        }
    }
}
