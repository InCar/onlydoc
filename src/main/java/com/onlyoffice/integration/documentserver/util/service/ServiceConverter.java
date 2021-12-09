/**
 * (c) Copyright Ascensio System SIA 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onlyoffice.integration.documentserver.util.service;

import org.json.simple.JSONObject;

import java.io.InputStream;

public interface ServiceConverter {
    String getConvertedUri(String documentUri, String fromExtension,
                           String toExtension, String documentRevisionId,
                           String filePass, Boolean isAsync);

    String generateRevisionId(String expectedKey);

    String convertStreamToString(InputStream stream);

    JSONObject convertStringToJSON(String jsonString);
}
