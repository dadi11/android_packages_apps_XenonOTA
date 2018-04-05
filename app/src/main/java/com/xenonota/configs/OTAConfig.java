/**
 * Copyright (C) 2018 XenonHD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xenonota.configs;

import android.content.Context;

import com.xenonota.utils.OTAUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Properties;

public class OTAConfig extends Properties {

    private final static String FILENAME = "ota_conf";

    private final static String OTA_OFFICIAL_URL = "ota_official";
    private final static String OTA_EXPERIMENTAL_URL = "ota_experimental";

    private final static String RELEASE_TYPE = "release_type";

    private final static String DEVICE_NAME = "device_name";

    private final static String VERSION_SOURCE = "version_source";
    private final static String VERSION_DELIMITER = "version_delimiter";
    private final static String VERSION_FORMAT = "version_format";
    private final static String VERSION_POSITION = "version_position";

    private static OTAConfig mInstance;

    private OTAConfig() {
    }

    public static OTAConfig getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new OTAConfig();
            try {
                InputStream is = context.getAssets().open(FILENAME);
                mInstance.load(is);
                is.close();
            } catch (IOException e) {
                OTAUtils.logError(e);
            }
        }
        return mInstance;
    }

    public String getOfficialOtaUrl() {
        return getProperty(OTAConfig.OTA_OFFICIAL_URL, "");
    }
    public String getExperimentalOtaUrl() {
        return getProperty(OTAConfig.OTA_EXPERIMENTAL_URL, "");
    }

    public String getReleaseType() {
        return getProperty(OTAConfig.RELEASE_TYPE, "Oreo");
    }

    String getVersionSource() {
        return getProperty(VERSION_SOURCE, getProperty("version_name", ""));
    }

    public String getDeviceSource() {
        return getProperty(OTAConfig.DEVICE_NAME, "");
    }

    String getDelimiter() {
        return getProperty(OTAConfig.VERSION_DELIMITER, "");
    }

    int getPosition() {
        int position;
        try {
            position = Integer.parseInt(getProperty(OTAConfig.VERSION_POSITION));
        } catch (NumberFormatException e) {
            position = -1;
        }
        return position;
    }

    SimpleDateFormat getFormat() {
        String format = getProperty(OTAConfig.VERSION_FORMAT, "");
        if (format.isEmpty()) {
            return null;
        }

        try {
            return new SimpleDateFormat(format, Locale.US);
        } catch (IllegalArgumentException | NullPointerException e) {
            OTAUtils.logError(e);
        }
        return null;
    }
}
