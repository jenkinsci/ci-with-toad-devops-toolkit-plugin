package com.quest.tdt;

import hudson.model.TaskListener;
import com.quest.tdt.util.StreamThread;
import com.quest.tdt.util.Constants;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ScriptPowerShell {
    private String connection;
    private String filePath;
    private int maxRows;
    private String outputPath;

    protected ScriptPowerShell(String connection, String filePath, int maxRows, String outputPath) {
        this.connection = connection;
        this.filePath = filePath;
        this.maxRows = maxRows;
        this.outputPath = outputPath;
    }

    public void run(TaskListener listener) throws IOException {
        InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(Constants.PS_S);

        // Create a temporary file to store our powershell resource stream.
        File script = File.createTempFile("tdt-s-", ".ps1");
        Files.copy(resourceStream, script.getAbsoluteFile().toPath(), REPLACE_EXISTING);

        // Create our command with appropriate arguments.
        String command = getProgram(script.getAbsolutePath())
                .concat(getConnectionArgument())
                .concat(getMaxRowsArgument())
                .concat(getFilePathArgument())
                .concat(getOutputPathArgument());

        listener.getLogger().println(Constants.LOG_HEADER_S + "Executing script...");

        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        process.getOutputStream().close();

        StreamThread outputStreamThread = new StreamThread(process.getInputStream(), listener, Constants.LOG_HEADER_S);
        StreamThread errorStreamThread = new StreamThread(process.getErrorStream(), listener, Constants.LOG_HEADER_S_ERR);

        outputStreamThread.start();
        errorStreamThread.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            listener.getLogger().println(Constants.LOG_HEADER_S_ERR + writer.toString());
        }

        // Clean up our temp script once we're finished.
        if (!script.delete()) {
            script.deleteOnExit();
        }

        listener.getLogger().println(Constants.LOG_HEADER_S + "Script completed");
    }

    private String getProgram(String path) { return "powershell ".concat(path); }

    private String getConnectionArgument() {
        return " -connection ".concat(Base64.getEncoder().encodeToString(connection.getBytes()));
    }

    private String getMaxRowsArgument() {
        return " -maxRows ".concat(Integer.toString(maxRows));
    }

    private String getFilePathArgument() {
        return " -inputFile ".concat(Base64.getEncoder().encodeToString(filePath.getBytes()));
    }

    private String getOutputPathArgument() {
        return outputPath.isEmpty() ? "" : " -outputFile ".concat(Base64.getEncoder().encodeToString(outputPath.getBytes()));
    }
}
