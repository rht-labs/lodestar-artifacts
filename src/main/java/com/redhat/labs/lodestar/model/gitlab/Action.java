package com.redhat.labs.lodestar.model.gitlab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Action {

    @Builder.Default
    private String action = "update";
    private String filePath;
    private String content;
    @Builder.Default
    private String encoding = "base64";

    public static class ActionBuilder {

        public ActionBuilder content(String content) {
            byte[] encodedContents = Base64.getEncoder().encode(content.getBytes());
            this.content = new String(encodedContents, StandardCharsets.UTF_8);
            return this;
        }
    }
}
