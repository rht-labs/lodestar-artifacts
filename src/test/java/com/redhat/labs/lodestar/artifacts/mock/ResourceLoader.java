package com.redhat.labs.lodestar.artifacts.mock;

import com.google.gson.Gson;
import com.redhat.labs.lodestar.artifacts.model.gitlab.File;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ResourceLoader {
    public static String load(String resourceName) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadGitlabFile(String gitlabResourceName) {
        String content = load(gitlabResourceName);

        File f = File.builder()
                .filePath("a.json").content(content).build();
        f.encodeFileAttributes();

        return new Gson().toJson(f);

    }
}
