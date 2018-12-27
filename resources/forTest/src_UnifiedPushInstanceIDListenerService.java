/**
 * JBoss, Home of Professional Open Source Copyright Red Hat, Inc., and
 * individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.jboss.aerogear.android.unifiedpush.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.iid.InstanceIDListenerService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import org.jboss.aerogear.android.core.Provider;
import org.jboss.aerogear.android.pipe.http.HttpProvider;
import org.jboss.aerogear.android.pipe.http.HttpRestProvider;
import static org.jboss.aerogear.android.unifiedpush.gcm.AeroGearGCMPushRegistrar.REGISTRAR_PREFERENCE_PATTERN;

/**
 * This is an Android Service which listens for InstanceID messages from
 * Google's GCM servicers.
 *
 * See
 * https://developers.google.com/instance-id/guides/android-implementation#refresh_tokens
 * for official docs
 *
 */
public class UnifiedPushInstanceIDListenerService extends InstanceIDListenerService {

    private final static String BASIC_HEADER = "Authorization";
    private final static String AUTHORIZATION_METHOD = "Basic";

    private static final String TAG = UnifiedPushInstanceIDListenerService.class.getSimpleName();
    private static final Integer TIMEOUT = 30000;// 30 seconds

    private final GCMSharedPreferenceProvider sharedPreferencesProvider = new GCMSharedPreferenceProvider();

    private final Provider<InstanceID> instanceIdProvider = new Provider<InstanceID>() {

        @Override
        public InstanceID get(Object... context) {
            return InstanceID.getInstance((Context) context[0]);
        }
    };

    private final Provider<HttpProvider> httpProviderProvider = new Provider<HttpProvider>() {

        @Override
        public HttpProvider get(Object... in) {
            return new HttpRestProvider((URL) in[0], (Integer) in[1]);
        }
    };

    @Override
    /**
     * This method is called when the Google Services have instructed us to
     * refresh out token states.
     */
    public void onTokenRefresh() {

        SharedPreferences sharedPreferences = sharedPreferencesProvider.get(this);

        Map<String, ?> preferences = sharedPreferences.getAll();

        for (Map.Entry<String, ?> preference : preferences.entrySet()) {
            if (preference.getKey().matches(REGISTRAR_PREFERENCE_PATTERN)) {
                String senderId = preference.getKey().split(":")[1];
                InstanceID instanceID = instanceIdProvider.get(this);
                try {
                    String regid = instanceID.getToken(senderId,
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                    JsonObject oldPostData = new JsonParser().parse(preference.getValue().toString()).getAsJsonObject();
                    URL deviceRegistryURL = new URL(oldPostData.get("deviceRegistryURL").getAsString());
                    String variantId = oldPostData.get("variantId").getAsString();
                    String secret = oldPostData.get("secret").getAsString();

                    HttpProvider httpProvider = httpProviderProvider.get(deviceRegistryURL, TIMEOUT);
                    setPasswordAuthentication(variantId, secret, httpProvider);

                    JsonObject postData = new JsonObject();
                    postData.addProperty("deviceType", oldPostData.get("deviceType").getAsString());
                    postData.addProperty("deviceToken", regid);
                    postData.addProperty("alias", oldPostData.get("alias").getAsString());
                    postData.addProperty("operatingSystem", oldPostData.get("operatingSystem").getAsString());
                    postData.addProperty("osVersion", oldPostData.get("osVersion").getAsString());

                    if (oldPostData.has("categories")) {
                        postData.add("categories", oldPostData.get("categories").getAsJsonArray());
                    }

                    httpProvider.post(postData.toString());

                } catch (IOException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                }

            }
        }

    }

    private void setPasswordAuthentication(final String username, final String password, final HttpProvider provider) {
        provider.setDefaultHeader(BASIC_HEADER, getHashedAuth(username, password.toCharArray()));
    }

    private String getHashedAuth(String username, char[] password) {
        StringBuilder headerValueBuilder = new StringBuilder(AUTHORIZATION_METHOD).append(" ");
        String unhashedCredentials = new StringBuilder(username).append(":").append(password).toString();
        String hashedCrentials = Base64.encodeToString(unhashedCredentials.getBytes(), Base64.DEFAULT | Base64.NO_WRAP);
        return headerValueBuilder.append(hashedCrentials).toString();
    }

}