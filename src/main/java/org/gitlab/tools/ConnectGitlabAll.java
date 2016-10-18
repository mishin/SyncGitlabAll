package org.gitlab.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabProject;

public class ConnectGitlabAll {

    public static void main(String[] args) {
        /**
         * Program clone git repository if they were not there or it them
         * synchronizes with the server version of the repository (fetch-and-merge)
         */
        ConnectGitlabAll gtLab = new ConnectGitlabAll();
        Properties prop = gtLab.readProperties();
        gtLab.gitlabConnect(prop);

    }

    private void gitlabConnect(Properties prop) {
        GitlabAPI api;
        api = GitlabAPI.connect(prop.getProperty("URL"), prop.getProperty("TOKEN"));
        try {
            List<GitlabProject> glabProg = api.getAllProjects();
            List<GitlabGroup> allGroup = api.getGroups();
            // We leave only groups that we have indicated in the properties file
            allGroup.forEach(groupBody -> {
                glabProg.forEach(projectBody -> {
                    generateSyncOrClone(projectBody, groupBody, prop);
                });
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateSyncOrClone(GitlabProject projectBody, GitlabGroup groupBody, Properties prop) {
        String groupWithName = projectBody.getPathWithNamespace();
        Path repositoryFolder = Paths.get(prop.getProperty("PATH") + "/" + groupWithName);
        String group = groupWithName.split("/")[0];
        // Если группа проекта входит в текущую группу
        if (group.equals(groupBody.getName()))
            makeSyncLine(repositoryFolder, projectBody);
    }

    private void makeSyncLine(Path repositoryFolder, GitlabProject projectBody) {
        if (!Files.exists(repositoryFolder)) {
            System.out.println("git clone " + projectBody.getSshUrl() + " " + repositoryFolder);
        } else {
            String baseLine = "git --git-dir=" + repositoryFolder + "\\.git --work-tree=" + repositoryFolder;
            System.out.println(baseLine + " fetch origin");
            System.out.println(baseLine + " merge origin/master");
        }
    }

    private Properties readProperties() {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream("config.properties");
            // load a properties file
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }
}
