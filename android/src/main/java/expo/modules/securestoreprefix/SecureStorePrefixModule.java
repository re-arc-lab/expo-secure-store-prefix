package expo.modules.securestoreprefix;

import java.net.URLEncoder;

import android.content.Context;
import android.content.SharedPreferences;

import org.unimodules.core.interfaces.ExpoMethod;

import expo.modules.securestore.SecureStoreModule;


public class SecureStorePrefixModule extends SecureStoreModule {
  private static final String SHARED_PREFERENCES_NAME = "SecureStore";

  private String mExperienceId;

  public SecureStorePrefixModule(Context context) {
    super(context);
  }

  @ExpoMethod
  @SuppressWarnings("unused")
  public void initAsync(String experienceId, Promise promise) {
    mExperienceId = URLEncoder.encode(experienceId, "UTF-8");
  }

  protected SharedPreferences getSharedPreferences() {
    return getContext().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
  }
}
