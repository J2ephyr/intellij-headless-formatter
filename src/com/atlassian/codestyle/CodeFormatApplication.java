package com.atlassian.codestyle;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.ExecUtil;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.highlighter.ProjectFileType;
import com.intellij.ide.impl.NewProjectUtil;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.idea.IdeaApplication;
import com.intellij.idea.Main;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.components.StorageScheme;
import com.intellij.openapi.externalSystem.service.project.wizard.SelectExternalProjectStep;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.projectImport.ProjectOpenProcessor;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.util.PlatformUtils;
import org.apache.log4j.BasicConfigurator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.sbt.project.SbtImportControl;
import org.jetbrains.sbt.project.SbtProjectImportProvider;
import org.jetbrains.sbt.project.SbtProjectOpenProcessor;
import org.jetbrains.sbt.project.settings.SbtProjectSettings;
import org.jetbrains.sbt.project.settings.SbtProjectSettingsControl;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

public class CodeFormatApplication extends IdeaApplication {

    private static File ideaDirectory;
    private static File ideaDirectoryBackup;

    public CodeFormatApplication(String[] args) {
        super(args);
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {

        BasicConfigurator.configure();

        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.getPlatformPrefix(PlatformUtils.IDEA_CE_PREFIX));
        System.setProperty("java.awt.headless", "true");
        System.setProperty("idea.is.unit.test", "true");

        final String pluginsPath = System.getProperty(PathManager.PROPERTY_PLUGINS_PATH);
        System.out.println("Plugins loaded from path: " + pluginsPath);
        Main.setFlags(null);

        final CodeFormatApplication app = new CodeFormatApplication(args);

        SwingUtilities.invokeLater(app::run);
    }

    private static void doCodeFormat(final String projectPAth) {
        try {
            System.out.println("Starting code format with: " + projectPAth);

            final Project project = setupProject(projectPAth);
            formatCode(project);

            System.out.println("Finished code format.");

            // This should work, but still seems to ask for confirmation which doesn't work in a headless environment
            // ApplicationManagerEx.getApplicationEx().exit(true, false);
            System.exit(0);
        } catch (Exception e) {
            // If for any reason we fail, we want to be able to carry on
            System.out.println("Failed to run code format job: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Project setupProject(final String projectPath) {
        System.out.println("Setting up project.");

        ApplicationManagerEx.getApplicationEx().doNotSave(false);

        final Project project;
        if (Files.exists(Paths.get(projectPath, ".idea"))) {
            project = setupIdeaProject(projectPath);
        } else if (Files.exists(Paths.get(projectPath, "pom.xml"))) {
            project = setupMavenProject(projectPath);
        } else if (Files.exists(Paths.get(projectPath, "build.sbt"))) {
            project = setupSbtProject(projectPath);
        } else {
            project = setupSourceProject(projectPath);
        }

        return project;
    }

    private static Project setupIdeaProject(String projectPath) {
        Project project;
        project = ProjectUtil.openProject(projectPath, null, false);

        refreshProject(project);
        return project;
    }

    private static Project setupMavenProject(String projectPath) {
        Project project;
        project = ProjectUtil.openOrImport(Paths.get(projectPath, "pom.xml").toString(), null, false);

        refreshProject(project);
        setupJdk(project);
        mavenImport(project);
        useSuppliedCodeStyleSettings(project);
        refreshProject(project);

        return project;
    }

    private static Project setupSbtProject(String projectPath) {
        Project project;
        project = openNewSbtProject(projectPath, null, false);
        //project = ProjectUtil.openOrImport(projectPath, null, false);

        setupJdk(project);
        useSuppliedCodeStyleSettings(project);
        refreshProject(project);

        return project;
    }

    private static Project setupSourceProject(String projectPath) {
        Project project;
        project = ProjectUtil.openOrImport(projectPath, null, false);

        refreshProject(project);
        setupJdk(project);
        useSuppliedCodeStyleSettings(project);
        refreshProject(project);

        return project;
    }

    private static void refreshProject(Project project) {
        if (project != null) {
            project.save();
        } else {
            System.out.println("Couldn't load project.");
            System.exit(1);
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
        });
    }

    private static void setupJdk(final Project project) {
        System.out.println("Setting up and indexing JDK.");
        ApplicationManager.getApplication().runWriteAction(() -> {

            final ProjectJdkImpl newJdk = new ProjectJdkImpl("JDK 1.8", JavaSdk.getInstance());
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
        mavenProjectsManager.importProjects();
    }

    private static void formatCode(final Project project) {

        final CodeStyleSettings codeStyleSettings = CodeStyleSettingsManager.getInstance(project).getCurrentSettings();
        codeStyleSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = Integer.MAX_VALUE;
        codeStyleSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND = Integer.MAX_VALUE;
        codeStyleSettings.JD_P_AT_EMPTY_LINES = false;

        final Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            System.out.println("Reformatting code for module: " + module.getName());
            final ReformatCodeProcessor processor = new ReformatCodeProcessor(project, module, false);
            processor.run();
        }

        FileDocumentManager.getInstance().saveAllDocuments();
    }

    private static void useSuppliedCodeStyleSettings(Project project) {
        File codeStyleSettings = new File(System.getProperty("code.style"));
        File ideaDirectory = new File(project.getBasePath(), ".idea");
        if (codeStyleSettings.exists() && ideaDirectory.exists()) {
            System.out.println("Copying codeStyleSettings.xml from " + codeStyleSettings.toPath());
            try {
                Files.copy(codeStyleSettings.toPath(), ideaDirectory.toPath().resolve(codeStyleSettings.toPath().getFileName()));
            } catch (IOException e) {
                System.out.println("Failed to run code format job: " + e);
                e.printStackTrace();
                System.exit(1);
            }
        }
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

    /**
     * @param path                project file path
     * @param projectToClose      currently active project
     * @param forceOpenInNewFrame forces opening in new frame
     * @return project by path if the path was recognized as IDEA project file or one of the project formats supported by
     * installed importers (regardless of opening/import result)
     * null otherwise
     */
    @Nullable
    public static Project openNewSbtProject(@NotNull String path, Project projectToClose, boolean forceOpenInNewFrame) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
        if (virtualFile == null) return null;
        virtualFile.refresh(false, false);

        ProjectOpenProcessor provider = ProjectOpenProcessor.getImportProvider(virtualFile);
        if (provider != null) {
            final Project project = doOpenProject((SbtProjectOpenProcessor) provider, virtualFile, projectToClose, forceOpenInNewFrame);

            if (project != null) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!project.isDisposed()) {
                            final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
                            if (toolWindow != null) {
                                toolWindow.activate(null);
                            }
                        }
                    }
                }, ModalityState.NON_MODAL);
            }

            return project;
        }

        return null;
    }

    @Nullable
    public static Project doOpenProject(@NotNull SbtProjectOpenProcessor provider, @NotNull VirtualFile virtualFile, Project projectToClose, boolean forceOpenInNewFrame) {
        try {
            provider.getBuilder().setUpdate(false);
            final WizardContext wizardContext = new WizardContext(null);
            if (virtualFile.isDirectory()) {
                final String[] supported = provider.getSupportedExtensions();
                for (VirtualFile file : getFileChildren(virtualFile)) {
                    if (canOpenFile(file, supported)) {
                        virtualFile = file;
                        break;
                    }
                }
            }

            wizardContext.setProjectFileDirectory(virtualFile.getParent().getPath());

            if (!doQuickImport(provider, virtualFile, wizardContext)) return null;

            if (wizardContext.getProjectName() == null) {
                if (wizardContext.getProjectStorageFormat() == StorageScheme.DEFAULT) {
                    wizardContext.setProjectName(IdeBundle.message("project.import.default.name", provider.getName()) + ProjectFileType.DOT_DEFAULT_EXTENSION);
                } else {
                    wizardContext.setProjectName(IdeBundle.message("project.import.default.name.dotIdea", provider.getName()));
                }
            }

            Project defaultProject = ProjectManager.getInstance().getDefaultProject();
            Sdk jdk = ProjectRootManager.getInstance(defaultProject).getProjectSdk();
            if (jdk == null) {
                jdk = ProjectJdkTable.getInstance().findMostRecentSdkOfType(JavaSdk.getInstance());
            }
            wizardContext.setProjectJdk(jdk);

            final String dotIdeaFilePath = wizardContext.getProjectFileDirectory() + File.separator + Project.DIRECTORY_STORE_FOLDER;
            final String projectFilePath = wizardContext.getProjectFileDirectory() + File.separator + wizardContext.getProjectName() +
                    ProjectFileType.DOT_DEFAULT_EXTENSION;

            File dotIdeaFile = new File(dotIdeaFilePath);
            File projectFile = new File(projectFilePath);

            String pathToOpen;
            if (wizardContext.getProjectStorageFormat() == StorageScheme.DEFAULT) {
                pathToOpen = projectFilePath;
            } else {
                pathToOpen = dotIdeaFile.getParent();
            }

            boolean shouldOpenExisting = false;
            boolean importToProject = false;
            if (projectFile.exists() || dotIdeaFile.exists()) {
                if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
                    shouldOpenExisting = true;
                    importToProject = true;
                } else {
                    String existingName;
                    if (dotIdeaFile.exists()) {
                        existingName = "an existing project";
                        pathToOpen = dotIdeaFile.getParent();
                    } else {
                        existingName = "'" + projectFile.getName() + "'";
                        pathToOpen = projectFilePath;
                    }
                    int result = Messages.showYesNoCancelDialog(
                            projectToClose,
                            IdeBundle.message("project.import.open.existing", existingName, projectFile.getParent(), virtualFile.getName()),
                            IdeBundle.message("title.open.project"),
                            IdeBundle.message("project.import.open.existing.openExisting"),
                            IdeBundle.message("project.import.open.existing.reimport"),
                            CommonBundle.message("button.cancel"),
                            Messages.getQuestionIcon());
                    if (result == Messages.CANCEL) return null;
                    shouldOpenExisting = result == Messages.YES;
                    importToProject = !shouldOpenExisting;
                }
            }

            final Project projectToOpen;
            if (shouldOpenExisting) {
                try {
                    projectToOpen = ProjectManagerEx.getInstanceEx().loadProject(pathToOpen);
                } catch (Exception e) {
                    return null;
                }
            } else {
                projectToOpen = ProjectManagerEx.getInstanceEx().newProject(wizardContext.getProjectName(), pathToOpen, true, false);
            }
            if (projectToOpen == null) return null;

            if (importToProject) {
                if (!provider.getBuilder().validate(projectToClose, projectToOpen)) {
                    return null;
                }

                projectToOpen.save();

                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        Sdk jdk = wizardContext.getProjectJdk();
                        if (jdk != null) {
                            NewProjectUtil.applyJdkToProject(projectToOpen, jdk);
                        }

                        String projectDirPath = wizardContext.getProjectFileDirectory();
                        String path = StringUtil.endsWithChar(projectDirPath, '/') ? projectDirPath + "classes" : projectDirPath + "/classes";
                        CompilerProjectExtension extension = CompilerProjectExtension.getInstance(projectToOpen);
                        if (extension != null) {
                            extension.setCompilerOutputUrl(SbtProjectOpenProcessor.getUrl(path));
                        }
                    }
                });

                provider.getBuilder().commit(projectToOpen, null, ModulesProvider.EMPTY_MODULES_PROVIDER);
            }

            if (!forceOpenInNewFrame) {
                NewProjectUtil.closePreviousProject(projectToClose);
            }
            ProjectUtil.updateLastProjectLocation(pathToOpen);
            ProjectManagerEx.getInstanceEx().openProject(projectToOpen);

            return projectToOpen;
        } finally {
            provider.getBuilder().cleanup();
        }
    }

    private static boolean doQuickImport(SbtProjectOpenProcessor provider, VirtualFile file, WizardContext wizardContext) {
        String path = SbtProjectImportProvider.projectRootOf(file).getPath();
        AddModuleWizard dialog = new AddModuleWizard(null, path, new SbtProjectImportProvider(provider.getBuilder()));

        provider.getBuilder().setFileToImport(file.getPath());
        provider.getBuilder().prepare(wizardContext);
        provider.getBuilder().getControl(null).setLinkedProjectPath(path);

        boolean result = setupSbtProjectInHeadlessMode(provider, new SbtProjectImportProvider(provider.getBuilder()), wizardContext);

        if (result && provider.getBuilder().getExternalProjectNode() != null) {
            wizardContext.setProjectName(provider.getBuilder().getExternalProjectNode().getData().getInternalName());
        }
        return result;
    }

    private static boolean setupSbtProjectInHeadlessMode(SbtProjectOpenProcessor provider,
                                                         SbtProjectImportProvider projectImportProvider,
                                                         WizardContext wizardContext) {
        final ModuleWizardStep[] wizardSteps = projectImportProvider.createSteps(wizardContext);
        if (wizardSteps.length > 0 && wizardSteps[0] instanceof SelectExternalProjectStep) {
            SelectExternalProjectStep selectExternalProjectStep = (SelectExternalProjectStep) wizardSteps[0];
            wizardContext.setProjectBuilder(provider.getBuilder());
            try {
                selectExternalProjectStep.updateStep();
                final SbtImportControl sbtImportControl = provider.getBuilder().getControl(wizardContext.getProject());

                SbtProjectSettingsControl sbtProjectSettingsControl =
                        (SbtProjectSettingsControl) sbtImportControl.getProjectSettingsControl();

                final SbtProjectSettings projectSettings = sbtProjectSettingsControl.getInitialSettings();

                projectSettings.setUseOurOwnAutoImport(true);
                sbtProjectSettingsControl.reset();

                if (!selectExternalProjectStep.validate()) {
                    return false;
                }
            } catch (ConfigurationException e) {
                System.out.println(wizardContext.getProject() + ": \n" + e.getMessage() + "\n" + e.getTitle());
                return false;
            }
            selectExternalProjectStep.updateDataModel();
        }
        return true;
    }

    private static Collection<VirtualFile> getFileChildren(VirtualFile file) {
        if (file instanceof NewVirtualFile) {
            return ((NewVirtualFile) file).getCachedChildren();
        }

        return Arrays.asList(file.getChildren());
    }

    private static boolean canOpenFile(VirtualFile file, String[] supported) {
        final String fileName = file.getName();
        for (String name : supported) {
            if (fileName.equals(name)) {
                return true;
            }
        }
        return false;
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

}