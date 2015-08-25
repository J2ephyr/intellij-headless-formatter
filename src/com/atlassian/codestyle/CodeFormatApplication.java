package com.atlassian.codestyle;

import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.codeInsight.actions.ReformatCodeAction;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
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
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.util.PlatformUtils;
import java.lang.reflect.InvocationTargetException;
import javax.swing.*;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

public class CodeFormatApplication extends IdeaApplication {

    public CodeFormatApplication(String[] args) {
        super(args);
    }

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

        final Project project = ProjectUtil.openOrImport(projectPomPath, null, false);
        if(project != null) {
            project.save();
        }
        else {
            System.out.println("Couldn't load project.");
            System.exit(1);
        }

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
            }
        });
        return project;
    }

    private static void setupJdk(final Project project) {
        System.out.println("Setting up and indexing JDK.");
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {

                final ProjectJdkImpl newJdk = new ProjectJdkImpl("1.8", JavaSdk.getInstance());
                newJdk.setHomePath("/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home");
                SdkType sdkType = (SdkType) newJdk.getSdkType();
                sdkType.setupSdkPaths(newJdk, null);
                ProjectJdkTable.getInstance().addJdk(newJdk);
                ProjectRootManager.getInstance(project).setProjectSdk(newJdk);
            }
        });
    }

    private static void mavenImport(final Project project) {
        System.out.println("Waiting for Maven import.");
        final MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
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
            final OptimizeImportsProcessor optimizeImportsProcessor = new OptimizeImportsProcessor(processor);

            // Reformat only Java classes
            ReformatCodeAction.registerFileMaskFilter(optimizeImportsProcessor, "*.java");
            optimizeImportsProcessor.run();
        }

        FileDocumentManager.getInstance().saveAllDocuments();
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {

        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.getPlatformPrefix(PlatformUtils.IDEA_CE_PREFIX));
        System.setProperty("java.awt.headless", "true");
        System.setProperty("idea.is.unit.test", "true");

        final String pluginsPath = System.getProperty(PathManager.PROPERTY_PLUGINS_PATH) ;
        System.out.println("Plugins loaded from path: " + pluginsPath);
        Main.setFlags(null);

        final CodeFormatApplication app = new CodeFormatApplication(args);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                app.run();
            }
        });

    }

}
