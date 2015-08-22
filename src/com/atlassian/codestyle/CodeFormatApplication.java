package com.atlassian.codestyle;

import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.idea.IdeaApplication;
import com.intellij.idea.Main;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.PlatformUtils;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

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
        System.out.println("Starting code format with pom.xml:" + projectPomPath);
        final Project project = ProjectUtil.openOrImport(projectPomPath, null, false);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
            }
        });

        final Module[] modules = ModuleManager.getInstance(project).getModules();
        for (int i = 0; i < modules.length; i++) {
            Module module = modules[i];
            final ReformatCodeProcessor processor = new ReformatCodeProcessor(project, module, false);
            final OptimizeImportsProcessor optimizeImportsProcessor = new OptimizeImportsProcessor(processor);
            optimizeImportsProcessor.run();
        }

        FileDocumentManager.getInstance().saveAllDocuments();
        System.out.println("Finished code format.");

        // This should work, but still seems to ask for confirmation which doesn't work in a headless environment
        // ApplicationManagerEx.getApplicationEx().exit(true, false);
        System.exit(0);
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {

        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.getPlatformPrefix(PlatformUtils.IDEA_CE_PREFIX));
        System.setProperty("java.awt.headless", "true");
        System.setProperty("idea.is.unit.test", "true");

        final String pluginsPath = System.getProperty("user.dir") + "/build/plugins";
        System.out.println("Plugins loaded from path:" + pluginsPath);
        System.out.println(System.getProperty("java.home"));
        System.setProperty(PathManager.PROPERTY_PLUGINS_PATH, pluginsPath);
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
