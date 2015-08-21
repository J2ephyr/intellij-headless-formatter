package com.atlassian.codestyle;

import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.idea.IdeaApplication;
import com.intellij.idea.Main;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.ide.PooledThreadExecutor;

import javax.swing.*;
import javax.swing.text.Document;
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
                System.out.println("Starting code formatting.");
//                ApplicationManagerEx.getApplicationEx().runWriteAction(new Runnable() {
//                    @Override
//                    public void run() {
//                        doCodeFormat();
//                    }
//                });
                doCodeFormat();
                FileDocumentManager.getInstance().saveAllDocuments();
                System.out.println("Done");
            }
        };
    }

    private static void doCodeFormat()
    {
        System.out.println("Running process.");
        final String projectPath = "/Users/marcosscriven/development/sources/atlassian-annotations/";

        ApplicationManagerEx.getApplicationEx().doNotSave(false);
        final Project project = ProjectUtil.openOrImport(projectPath, null, false);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
            }
        });

        final Module[] modules = ModuleManager.getInstance(project).getModules();
        final ReformatCodeProcessor processor = new ReformatCodeProcessor(project, modules[2], false);
        System.out.println("Before run");
        final OptimizeImportsProcessor optimizeImportsProcessor = new OptimizeImportsProcessor(processor);
        System.out.println("Prepared processor.");
        optimizeImportsProcessor.run();
        System.out.println("After run");
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        final String[] emptyArgs = new String[] {};
        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.getPlatformPrefix(PlatformUtils.IDEA_CE_PREFIX));
        System.setProperty("java.awt.headless", "true");
        System.setProperty("idea.is.unit.test", "true");
        Main.setFlags(emptyArgs);
        final CodeFormatApplication app = new CodeFormatApplication(emptyArgs);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
//                PluginManager.installExceptionHandler();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        app.run();
                        System.out.println("here");
                    }
                });
                System.out.println("here2");
            }
        });
        System.out.println("End");

    }

}
