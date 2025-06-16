/**
 * Copyright © 2014-2025 PDF Technologies, Inc. All Rights Reserved.
 * <p>
 * THIS SOURCE CODE AND ANY ACCOMPANYING DOCUMENTATION ARE PROTECTED BY INTERNATIONAL COPYRIGHT LAW
 * AND MAY NOT BE RESOLD OR REDISTRIBUTED. USAGE IS BOUND TO THE ComPDFKit LICENSE AGREEMENT.
 * UNAUTHORIZED REPRODUCTION OR DISTRIBUTION IS SUBJECT TO CIVIL AND CRIMINAL PENALTIES.
 * This notice may not be removed from this file.
 */

package com.compdfkit.flutter.compdfkit_flutter.plugin;

import static com.compdfkit.flutter.compdfkit_flutter.constants.CPDFConstants.ChannelMethod.CREATE_DOCUMENT_PLUGIN;
import static com.compdfkit.flutter.compdfkit_flutter.constants.CPDFConstants.ChannelMethod.CREATE_URI;
import static com.compdfkit.flutter.compdfkit_flutter.constants.CPDFConstants.ChannelMethod.GET_TEMP_DIRECTORY;
import static com.compdfkit.flutter.compdfkit_flutter.constants.CPDFConstants.ChannelMethod.INIT_SDK;
import static com.compdfkit.flutter.compdfkit_flutter.constants.CPDFConstants.ChannelMethod.INIT_SDK_KEYS;
import static com.compdfkit.flutter.compdfkit_flutter.constants.CPDFConstants.ChannelMethod.OPEN_DOCUMENT;
import static com.compdfkit.flutter.compdfkit_flutter.constants.CPDFConstants.ChannelMethod.PICK_FILE;
import static com.compdfkit.flutter.compdfkit_flutter.constants.CPDFConstants.ChannelMethod.REMOVE_SIGN_FILE_LIST;
import static com.compdfkit.flutter.compdfkit_flutter.constants.CPDFConstants.ChannelMethod.SDK_BUILD_TAG;
import static com.compdfkit.flutter.compdfkit_flutter.constants.CPDFConstants.ChannelMethod.SDK_VERSION_CODE;
import static com.compdfkit.flutter.compdfkit_flutter.constants.CPDFConstants.ChannelMethod.SET_IMPORT_FONT_DIRECTORY;
import static com.compdfkit.flutter.compdfkit_flutter.constants.CPDFConstants.ChannelMethod.CLOSE_DOCUMENT;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.compdfkit.core.document.CPDFDocument;
import com.compdfkit.core.document.CPDFSdk;
import com.compdfkit.core.font.CPDFFont;
import com.compdfkit.flutter.compdfkit_flutter.utils.FileUtils;
import com.compdfkit.tools.common.pdf.CPDFConfigurationUtils;
import com.compdfkit.tools.common.pdf.CPDFDocumentActivity;
import com.compdfkit.tools.common.pdf.config.CPDFConfiguration;
import com.compdfkit.tools.common.utils.CFileUtils;

import com.compdfkit.tools.common.utils.CUriUtil;
import io.flutter.plugin.common.PluginRegistry;
import java.io.File;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;


public class ComPDFKitSDKPlugin extends BaseMethodChannelPlugin implements PluginRegistry.ActivityResultListener {

    private static final int REQUEST_CODE = (ComPDFKitSDKPlugin.class.hashCode() + 43) & 0x0000ffff;

    private Activity activity;
    private Application application;
    private MethodChannel.Result pendingResult;
    private BinaryMessenger binaryMessenger;
    private MethodChannel methodChannel;

    public ComPDFKitSDKPlugin(Activity activity, BinaryMessenger binaryMessenger) {
        super(activity, binaryMessenger);
        this.activity = activity;
        this.application = activity.getApplication();
        this.binaryMessenger = binaryMessenger;
        this.methodChannel = new MethodChannel(binaryMessenger, "com.compdfkit.flutter.plugin");

        registerActivityLifecycleCallbacks();
    }

    @Override
    public String methodName() {
        return "com.compdfkit.flutter.plugin";
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            case INIT_SDK:
                String key = call.argument("key");
                CPDFSdk.init(context, key, true, (code, msg) ->
                        Log.e("ComPDFKit-Plugin", "INIT_SDK: code:" + code + ", msg:" + msg)
                );
                break;
            case INIT_SDK_KEYS:
                String androidLicenseKey = call.argument("androidOnlineLicense");
                CPDFSdk.init(context, androidLicenseKey, false, (code, msg) ->
                        Log.e("ComPDFKit-Plugin", "INIT_SDK_KEYS: code:" + code + ", msg:" + msg)
                );
                break;
            case SDK_VERSION_CODE:
                result.success(CPDFSdk.getSDKVersion());
                break;
            case SDK_BUILD_TAG:
                result.success(CPDFSdk.getSDKBuildTag());
                break;
            case OPEN_DOCUMENT:
                String filePath = call.argument("document");
                String password = call.argument("password");
                String configurationJson = call.argument("configuration");
                CPDFConfiguration configuration = CPDFConfigurationUtils.fromJson(configurationJson);
                Intent intent = new Intent(context, CPDFDocumentActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                FileUtils.parseDocument(context, filePath, intent);
                Log.e("SDL1", getPassword());
                Log.e("SDL2", password);
                Log.e("SDL3", decryptText(getPassword(), password) + "");
                intent.putExtra(CPDFDocumentActivity.EXTRA_FILE_PASSWORD, password.length() > 0 ? decryptText(getPassword(), password): password);
                intent.putExtra(CPDFDocumentActivity.EXTRA_CONFIGURATION, configuration);
                context.startActivity(intent);
                break;
            case GET_TEMP_DIRECTORY:
                result.success(context.getCacheDir().getAbsolutePath());
                break;
            case REMOVE_SIGN_FILE_LIST:
                File dirFile = new File(context.getFilesDir(), CFileUtils.SIGNATURE_FOLDER);
                boolean deleteResult = CFileUtils.deleteDirectory(dirFile.getAbsolutePath());
                result.success(deleteResult);
                break;
            case PICK_FILE:
                pendingResult = result;
                if (activity != null) {
                    activity.startActivityForResult(CFileUtils.getContentIntent(), REQUEST_CODE);
                }
                break;
            case CREATE_URI:
                String fileName = call.argument("file_name");
                String mimeType = call.argument("mime_type");
                String childDirectoryName = call.argument("child_directory_name");
                String dir = Environment.DIRECTORY_DOWNLOADS;
                if (!TextUtils.isEmpty(childDirectoryName)) {
                    dir += File.separator + childDirectoryName;
                }
                Uri uri = CUriUtil.createFileUri(context, dir, fileName, mimeType);
                if (uri != null) {
                    result.success(uri.toString());
                } else {
                    result.error("CREATE_URI_FAIL", "create uri fail", "");
                }
                break;
            case SET_IMPORT_FONT_DIRECTORY:
                String importFontDir = call.argument("dir_path");
                boolean addSysFont = call.argument("add_sys_font");
                CPDFSdk.setImportFontDir(importFontDir, addSysFont);
                result.success(true);
                break;
            case CREATE_DOCUMENT_PLUGIN:
                String id = (String) call.arguments;
                CPDFDocumentPlugin documentPlugin = new CPDFDocumentPlugin(context, binaryMessenger, id);
                documentPlugin.setDocument(new CPDFDocument(context));
                documentPlugin.register();
                result.success(true);
                break;
            default:
                result.notImplemented();
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                CFileUtils.takeUriPermission(context, uri);
                successResult(uri.toString());
            }
            return true;
        } else if (requestCode == REQUEST_CODE) {
            successResult(null);
        }
        return false;
    }

    private void successResult(String uri) {
        if (pendingResult != null) {
            pendingResult.success(uri);
        }
        clearPendingResult();
    }

    private void clearPendingResult() {
        pendingResult = null;
    }

    private void registerActivityLifecycleCallbacks() {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (activity.getClass().getName().equals("com.compdfkit.tools.common.pdf.CPDFDocumentActivity")) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        methodChannel.invokeMethod(CLOSE_DOCUMENT, null);
                    });

                }
            }
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
                if (activity.getClass().getName().equals("com.compdfkit.tools.common.pdf.CPDFDocumentActivity")) {
                    activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE);
                }
            }
            @Override
            public void onActivityStarted(Activity activity) {}
            @Override
            public void onActivityResumed(Activity activity) {}
            @Override
            public void onActivityPaused(Activity activity) {}
            @Override
            public void onActivityStopped(Activity activity) {}
            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}
        });
    }

    public static byte[] utf8ToHexBytes(String input, boolean havePadding) {
        StringBuilder hex = new StringBuilder();
        for (byte b : input.getBytes(StandardCharsets.UTF_8)) {
            hex.append(String.format("%02x", b));
        }

        if (havePadding) {
            // pad to 32 characters (16 bytes)
            while (hex.length() < 32) {
                hex.append("0");
            }
            hex.setLength(32);
        }

        return hexStringToBytes(hex.toString());
    }

    private String decryptText(String key, String text) {
        try {
            // Step 1: Key and IV derivation
            byte[] keyBytes = utf8ToHexBytes(key, false); // 32 bytes
            byte[] ivBytes = utf8ToHexBytes(key.substring(0, 4), true); // padded

            // Step 2: Build AES Key/IV
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            // Step 3: Base64 decode the ciphertext
            byte[] cipherBytes = Base64.decode(text, Base64.DEFAULT);

            // Step 4: Decrypt
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
            byte[] decryptedBytes = cipher.doFinal(cipherBytes);

            // Remove any potential padding manually (if used)
            return new String(decryptedBytes, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private final byte[] psBytes = {
            99, 81, 57, 65, 126, 62, 56, 43,
            75, 58, 79, 127, 108, 59, 106, 122
    };

    private String getPassword() {
        byte[] decodedBytes = new byte[psBytes.length];

        for (int i = 0; i < psBytes.length; i++) {
            decodedBytes[i] = (byte)(psBytes[i] ^ 8);
        }

        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}
