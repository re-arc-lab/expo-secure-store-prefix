# expo-secure-store-prefix

## 背景

Expo SDK のバージョンを40から42（eas build）に上げた際に、[expo-secure-store](https://github.com/expo/expo/tree/master/packages/expo-secure-store)を使って保存していたデータにアクセスできなくなったので、引き続きアクセスできるように prefix 対応を行った。

## expo-secure-store からの改変内容（主要なもののみを抜粋）

### JS

* prefix 対応を行うための `initAsync()` メソッドを用意。
* iOS では既存の expo-secure-store を利用した上で prefix 対応を行う。

```diff
--- expo-secure-store/src/SecureStore.ts
+++ ./src/SecureStorePrefix.ts
@@ -1,6 +1,14 @@
 import { UnavailabilityError } from '@unimodules/core';
+import { Platform } from 'react-native';

-import ExpoSecureStore from './ExpoSecureStore';
+import ExpoSecureStoreWithoutPrefix from './ExpoSecureStore';
+import ExpoSecureStorePrefix from './ExpoSecureStorePrefix';
+
+// Android: 専用の ExpoSecureStorePrefix を使う。
+// iOS: JS レイヤーで prefix 対応をするので、ネイティブレイヤーでは既存の ExpoSecureStore を使う。
+const ExpoSecureStore = Platform.OS === 'ios' ? ExpoSecureStoreWithoutPrefix : ExpoSecureStorePrefix;
+
+let prefix: string | undefined;

 export type KeychainAccessibilityConstant = number;

@@ -86,6 +94,13 @@
   return !!ExpoSecureStore.getValueWithKeyAsync;
 }

+export async function initAsync(experienceId: string): Promise<void> {
+  prefix = experienceId;
+  if (!!ExpoSecureStore.initAsync) {
+    await ExpoSecureStore.initAsync(experienceId);
+  }
+}
+
 // @needsAudit
 /**
  * Delete the value associated with the provided key.
@@ -104,7 +119,7 @@
   if (!ExpoSecureStore.deleteValueWithKeyAsync) {
     throw new UnavailabilityError('SecureStore', 'deleteItemAsync');
   }
-  await ExpoSecureStore.deleteValueWithKeyAsync(key, options);
+  await ExpoSecureStore.deleteValueWithKeyAsync(_getNativeKey(key), options);
 }

 // @needsAudit
@@ -122,7 +137,7 @@
   options: SecureStoreOptions = {}
 ): Promise<string | null> {
   _ensureValidKey(key);
-  return await ExpoSecureStore.getValueWithKeyAsync(key, options);
+  return await ExpoSecureStore.getValueWithKeyAsync(_getNativeKey(key), options);
 }

 // @needsAudit
@@ -150,7 +165,7 @@
   if (!ExpoSecureStore.setValueWithKeyAsync) {
     throw new UnavailabilityError('SecureStore', 'setItemAsync');
   }
-  await ExpoSecureStore.setValueWithKeyAsync(value, key, options);
+  await ExpoSecureStore.setValueWithKeyAsync(value, _getNativeKey(key), options);
 }

 function _ensureValidKey(key: string) {
@@ -161,6 +176,11 @@
   }
 }

+function _getNativeKey(key: string) {
+  // iOS はここで prefix 対応。
+  return Platform.OS === 'ios' ? `${prefix}-${key}` : key;
+}
+
 function _isValidKey(key: string) {
   return typeof key === 'string' && /^[\w.-]+$/.test(key);
 }
```

### Android

* expo-secure-store@10.2.0 から SecureStoreModule を別名前空間にコピー。

```diff
--- expo-secure-store/android/src/main/java/expo/modules/securestore/SecureStoreModule.java
+++ ./android/src/main/java/expo/modules/securestore_10_2/SecureStoreModule.java
@@ -1,4 +1,4 @@
-package expo.modules.securestore;
+package expo.modules.securestore_10_2;

 import android.annotation.SuppressLint;
 import android.annotation.TargetApi;
@@ -88,7 +88,7 @@
       return;
     }

-    SharedPreferences prefs = getContext().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
+    SharedPreferences prefs = getSharedPreferences();

     if (value == null) {
       boolean success = prefs.edit().putString(key, null).commit();
```

* SecureStoreModule を継承した prefix 対応クラスを作成。

```java
public class SecureStorePrefixModule extends SecureStoreModule {
  private static final String TAG = "ExpoSecureStorePrefix";
  private static final String SHARED_PREFERENCES_NAME = "SecureStore";

  private String mPrefix;

  public SecureStorePrefixModule(Context context) {
    super(context);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @ExpoMethod
  @SuppressWarnings("unused")
  public void initAsync(String experienceId, Promise promise) {
    try {
      // 互換性を保つためにエンコード。host.exp.exponent.utils.ScopedContext を参考。
      mPrefix = URLEncoder.encode(experienceId, "UTF-8");
      promise.resolve(null);
    } catch (UnsupportedEncodingException e) {
      Log.e(TAG, "Caught unexpected exception when init from SecureStorePrefix", e);
      promise.reject("E_SECURESTOREPREFIX_INIT_ERROR", "An unexpected error occurred when init from SecureStorePrefix", e);
    }
  }

  @Override
  protected SharedPreferences getSharedPreferences() {
    return getContext().getSharedPreferences(mPrefix + "-" +  SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
  }
}
```

## その他

過去バージョンと Keychain アクセスを共有できるように、app.json で `entitlements.keychain-access-groups` の設定を忘れないこと。
