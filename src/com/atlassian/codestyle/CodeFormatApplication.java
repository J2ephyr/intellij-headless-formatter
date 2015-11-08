package com.atlassian.codestyle;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.ExecUtil;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.idea.IdeaApplication;
import com.intellij.idea.Main;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class CodeFormatApplication extends IdeaApplication {

    public CodeFormatApplication(String[] args) {
        super(args);
    }

    @NotNull
    @Override
    public ApplicationStarter getStarter() {

        return new ApplicationStarter() {
            @Override
            public String getCommandName() {
                return "codeformat";
            }

            @Override
            public void premain(String[] args) {
                // Nothing to do
            }

            @Override
            public void main(String[] args) {
                doCodeFormat(args[0]);
            }
        };
    }

    private static void doCodeFormat(final String projectPomPath)
    {
        try {
            System.out.println("Starting code format with pom.xml: " + projectPomPath);

            final Project project = setupProject(projectPomPath);
            setupJdk(project);
            mavenImport(project);
            formatCode(project);

            System.out.println("Finished code format.");

            // This should work, but still seems to ask for confirmation which doesn't work in a headless environment
            // ApplicationManagerEx.getApplicationEx().exit(true, false);
            System.exit(0);
        }
        catch (Exception e) {
            // If for any reason we fail, we want to be able to carry on
            System.out.println("Failed to run code format job: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Project setupProject(final String projectPomPath) {
        System.out.println("Setting up project.");
        ApplicationManagerEx.getApplicationEx().doNotSave(false);

        final Project project = ProjectUtil.openProject(projectPomPath, null, false);
        if(project != null) {
            project.save();
        }
        else {
            System.out.println("Couldn't load project.");
            System.exit(1);
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
        });
        return project;
    }

    private static void setupJdk(final Project project) {
        System.out.println("Setting up and indexing JDK.");
        ApplicationManager.getApplication().runWriteAction(() -> {

            final ProjectJdkImpl newJdk = new ProjectJdkImpl("1.8", JavaSdk.getInstance());
            newJdk.setHomePath(suggestHomePath());
            SdkType sdkType = (SdkType) newJdk.getSdkType();
            sdkType.setupSdkPaths(newJdk, null);
            ProjectJdkTable.getInstance().addJdk(newJdk);
            ProjectRootManager.getInstance(project).setProjectSdk(newJdk);
        });
    }

    private static void mavenImport(final Project project) {
        final MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);

        System.out.println("Waiting for Maven import.");
        mavenProjectsManager.waitForResolvingCompletion();
        MavenProjectsManager.getInstance(project).importProjects();
        project.save();
    }

    private static void formatCode(final Project project) {
        final CodeStyleSettings codeStyleSettings = CodeStyleSettingsManager.getInstance().getCurrentSettings();

        codeStyleSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND=Integer.MAX_VALUE;
        codeStyleSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND=Integer.MAX_VALUE;
        codeStyleSettings.JD_P_AT_EMPTY_LINES=false;

        final Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            System.out.println("Reformatting code for module: " + module.getName());
            final ReformatCodeProcessor processor = new ReformatCodeProcessor(project, module, false);
            processor.run();
        }

        FileDocumentManager.getInstance().saveAllDocuments();
    }

    @Nullable
    private static String suggestHomePath() {
        if (SystemInfo.isMac) {
            if (new File("/usr/libexec/java_home").canExecute()) {
                String path = ExecUtil.execAndReadLine(new GeneralCommandLine("/usr/libexec/java_home"));
                if (path != null && new File(path).isDirectory()) {
                    return path;
                }
            }

            String home = checkKnownLocations("/Library/Java/JavaVirtualMachines", "/System/Library/Java/JavaVirtualMachines");
            if (home != null) return home;
        }

        if (SystemInfo.isLinux) {
            String home = checkKnownLocations("/usr/lib/jvm/java-8-jdk");
            if (home != null) return home;
        }

        String property = System.getProperty("java.home");
        if (property != null) {
            File javaHome = new File(property);
            if (javaHome.getName().equals("jre")) {
                javaHome = javaHome.getParentFile();
            }
            if (javaHome != null && javaHome.isDirectory()) {
                return javaHome.getAbsolutePath();
            }
        }

        return null;
    }

    @Nullable
    private static String checkKnownLocations(String... locations) {
        for (String home : locations) {
            if (new File(home).isDirectory()) {
                return home;
            }
        }

        return null;
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {

        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.getPlatformPrefix(PlatformUtils.IDEA_CE_PREFIX));
        System.setProperty("java.awt.headless", "true");
        System.setProperty("idea.is.unit.test", "true");

        final String pluginsPath = System.getProperty(PathManager.PROPERTY_PLUGINS_PATH) ;
        System.out.println("Plugins loaded from path: " + pluginsPath);
        Main.setFlags(null);

        final CodeFormatApplication app = new CodeFormatApplication(args);

        SwingUtilities.invokeLater(app::run);
    }
}