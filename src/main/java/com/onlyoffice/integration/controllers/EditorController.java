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

package com.onlyoffice.integration.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.integration.documentserver.managers.history.HistoryManager;
import com.onlyoffice.integration.documentserver.managers.jwt.JwtManager;
import com.onlyoffice.integration.documentserver.models.enums.Action;
import com.onlyoffice.integration.documentserver.models.enums.Language;
import com.onlyoffice.integration.documentserver.models.enums.Type;
import com.onlyoffice.integration.documentserver.models.filemodel.Document;
import com.onlyoffice.integration.documentserver.models.filemodel.EditorConfig;
import com.onlyoffice.integration.documentserver.models.filemodel.FileModel;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.documentserver.util.Misc;
import com.onlyoffice.integration.dto.Mentions;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.services.UserServices;
import com.onlyoffice.integration.services.configurers.FileConfigurer;
import com.onlyoffice.integration.services.configurers.wrappers.DefaultFileWrapper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin("*")
@Controller
public class EditorController {

    private Logger logger = LoggerFactory.getLogger(EditorController.class);

    @Value("${files.docservice.url.site}")
    private String docserviceSite;

    @Value("${files.docservice.url.api}")
    private String docserviceApiUrl;

    @Autowired
    private FileStoragePathBuilder storagePathBuilder;

    @Autowired
    private JwtManager jwtManager;

    @Autowired
    private UserServices userService;

    @Autowired
    private HistoryManager historyManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileConfigurer<DefaultFileWrapper> fileConfigurer;

    @GetMapping(path = "${url.editor}")
    public String index(@RequestParam("fileName") String fileName,
                        @RequestParam(value = "action", required = false) String actionParam,
                        @RequestParam(value = "type", required = false) String typeParam,
                        @RequestParam(value = "actionLink", required = false) String actionLink,
                        @RequestParam(value = "fileKey", required = false) String fileKey,
                        @RequestParam(value = "fileUrl", required = false) String fileUrl,
                        @RequestParam(value = "thirdUri", required = false) Boolean thirdUri,
                        @RequestParam(value = "lang", required = false) String lang,
                        @RequestParam(value = "displayName", required = false) String displayName,
                        @CookieValue(value = "uid", required = false) String uid,
                        @CookieValue(value = "ulang", required = false) String ulang,
                        Model model) throws JsonProcessingException {
        Action action = Action.edit;
        Type type = Type.desktop;
        Language language = Language.en;

        if (actionParam != null) {
            action = Action.valueOf(actionParam);
        }
        if (typeParam != null) {
            type = Type.valueOf(typeParam);
        }
        if (lang != null) {
            language = Language.valueOf(lang);
        } else if (ulang != null) {
            language = Language.valueOf(ulang);
        }

        // Third UID
        // Default John Smith (uid=1).
        // com.onlyoffice.integration.documentserver.managers.history.DefaultHistoryManager#L84
        if (StringUtils.isBlank(uid)) {
            uid = "1";
        }

        Optional<User> optionalUser = userService.findUserById(Integer.parseInt(uid));

        if (!optionalUser.isPresent()) {
            return "index.html";
        }

        User user = optionalUser.get();

        FileModel fileModel = fileConfigurer.getFileModel(
                DefaultFileWrapper
                        .builder()
                        .fileName(fileName)
                        .type(type)
                        .lang(language)
                        .action(action)
                        .user(user)
                        .actionData(actionLink)
                        .build()
        );

        // Third URI
        if (null != thirdUri && thirdUri) {
            Document document = fileModel.getDocument();
            if (StringUtils.isNotBlank(fileKey)) {
                document.setKey(fileKey);
            } else {
                document.setKey(fileName);
            }
            document.setUrl(fileUrl);
            document.getPermissions().setEdit(false);
            document.getPermissions().setComment(false);

            EditorConfig editorConfig = fileModel.getEditorConfig();
            if (StringUtils.isBlank(displayName)) {
                displayName = "匿名";
            }
            editorConfig.getUser().setName(displayName);
            editorConfig.getCustomization().setChat(false);
            editorConfig.getCustomization().setComments(false);

            logger.info("{} -> {}({}) -> {}", displayName, fileName, document.getKey(), document.getUrl());
        }

        model.addAttribute("model", fileModel);
        model.addAttribute("fileHistory", historyManager.getHistory(fileModel.getDocument()));
        model.addAttribute("docserviceApiUrl", docserviceSite + docserviceApiUrl);
        model.addAttribute("dataInsertImage", getInsertImage());
        model.addAttribute("dataCompareFile", getCompareFile());
        model.addAttribute("dataMailMergeRecipients", getMailMerge());
        model.addAttribute("usersForMentions", getUserMentions(uid));

        return "editor.html";
    }

    private List<Mentions> getUserMentions(String uid) {
        List<Mentions> usersForMentions = new ArrayList<>();
        if (uid != null && !uid.equals("4")) {
            List<User> list = userService.findAll();
            for (User u : list) {
                if (u.getId() != Integer.parseInt(uid) && u.getId() != 4) {
                    usersForMentions.add(new Mentions(u.getName(), u.getEmail()));
                }
            }
        }

        return usersForMentions;
    }

    @SneakyThrows
    private String getInsertImage() {
        Map<String, Object> dataInsertImage = new HashMap<>();
        dataInsertImage.put("fileType", "png");
        dataInsertImage.put("url", storagePathBuilder.getServerUrl(true) + "/css/img/logo.png");

        if (jwtManager.tokenEnabled()) {
            dataInsertImage.put("token", jwtManager.createToken(dataInsertImage));
        }

        return objectMapper.writeValueAsString(dataInsertImage).substring(1, objectMapper.writeValueAsString(dataInsertImage).length() - 1);
    }

    @SneakyThrows
    private String getCompareFile() {
        Map<String, Object> dataCompareFile = new HashMap<>();
        dataCompareFile.put("fileType", "docx");
        dataCompareFile.put("url", storagePathBuilder.getServerUrl(true) + "/assets?name=sample.docx");

        if (jwtManager.tokenEnabled()) {
            dataCompareFile.put("token", jwtManager.createToken(dataCompareFile));
        }

        return objectMapper.writeValueAsString(dataCompareFile);
    }

    @SneakyThrows
    private String getMailMerge() {
        Map<String, Object> dataMailMergeRecipients = new HashMap<>();
        dataMailMergeRecipients.put("fileType", "csv");
        dataMailMergeRecipients.put("url", storagePathBuilder.getServerUrl(true) + "/csv");

        if (jwtManager.tokenEnabled()) {
            dataMailMergeRecipients.put("token", jwtManager.createToken(dataMailMergeRecipients));
        }

        return objectMapper.writeValueAsString(dataMailMergeRecipients);
    }
}
