package expo.modules.securestoreprefix;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.unimodules.core.Promise;
import org.unimodules.core.interfaces.ExpoMethod;

import expo.modules.securestore.SecureStoreModule;


public class SecureStorePrefixModule extends SecureStoreModule {
  private static final String TAG = "ExpoSecureStorePrefix";
  private static final String SHARED_PREFERENCES_NAME = "SecureStore";

  private String mExperienceId;

  public SecureStorePrefixModule(Context context) {
    super(context);
  }

  @ExpoMethod
  @SuppressWarnings("unused")
  public void initAsync(String experienceId, Promise promise) {
    try {
      mExperienceId = URLEncoder.encode(experienceId, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      Log.e(TAG, "Caught unexpected exception when init from SecureStorePrefix", e);
      promise.reject("E_SECURESTOREPREFIX_INIT_ERROR", "An unexpected error occurred when init from SecureStorePrefix", e);
    }
  }

  protected SharedPreferences getSharedPreferences() {
    return getContext().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
  }
}
