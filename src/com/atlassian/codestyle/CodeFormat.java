package com.atlassian.codestyle;

import com.intellij.ExtensionPoints;
import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.ide.impl.PatchProjectUtil;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.idea.CommandLineApplication;
import com.intellij.idea.IdeaApplication;
import com.intellij.idea.Main;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.command.impl.UndoManagerImpl;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.PlatformUtils;
import org.jdom.JDOMException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoManager;

public class CodeFormat
{

    public static void main(String[] args) throws IOException, InvocationTargetException, InterruptedException, JDOMException, InvalidDataException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_CE_PREFIX);
        ApplicationManagerEx.createApplication(false, false, true, false, ApplicationManagerEx.IDEA_APPLICATION, null);


        final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    app.load(null);
                } catch (Exception e) {
                    System.out.println("Couldn't spin up headless IntelliJ.");
                }
            }
        });

//        app.doNotSave();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                final String projectPath = "/Users/marcosscriven/development/sources/atlassian-annotations";
                ApplicationManagerEx.getApplicationEx().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        final Project project = ProjectUtil.openOrImport(projectPath, null, false);
                    }
                });
                ApplicationManagerEx.getApplicationEx().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        final Project project = ProjectManager.getInstance().getOpenProjects()[0];
                        final Module[] modules = ModuleManager.getInstance(project).getModules();
                        final ReformatCodeProcessor processor = new ReformatCodeProcessor(project, modules[2], false);
                        new OptimizeImportsProcessor(processor).run();
                    }
                });
            }
        });
        System.out.println("Done");
    }

//    Constructor<CommandLineApplication> constructor = (Constructor<CommandLineApplication>) CommandLineApplication.class.getDeclaredConstructors()[0];
//    constructor.setAccessible(true);
//    final CommandLineApplication cla = constructor.newInstance(false, false, true);

}
