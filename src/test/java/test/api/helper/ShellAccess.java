package test.api.helper;

import java.io.IOException;


public class ShellAccess {

    public static void syncExec(String... command)
            throws Exception {

        Process process = new ProcessBuilder(command).inheritIO().start();
        int exitCode = process.waitFor();

        if (exitCode != 0)
            throw new IOException("The command " + String.join(" ", command) + " quits with exit code " + exitCode);
    }
}
