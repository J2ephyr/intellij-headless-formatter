package com.atlassian.codestyle;

import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class CodeFormatStarter implements ApplicationStarter {
    @Override
    public String getCommandName() {
        return "codeformat";
    }

    @Override
    public void premain(String[] args) {
        final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
        try {
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
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void main(String[] args) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
