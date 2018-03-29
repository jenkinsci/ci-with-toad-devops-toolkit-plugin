package com.quest.tdt;

import com.quest.tdt.util.Constants;
import com.quest.tdt.util.StreamThread;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.StringJoiner;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class UnitTestPowerShell {
    private String connection;
    private List<UnitTestDBObject> objects;
    private String folder;
    private boolean txt;
    private boolean xml;

    public UnitTestPowerShell(String connection, List<UnitTestDBObject> objects, String folder, boolean txt, boolean xml) {
        this.connection = connection;
        this.objects = objects;
        this.folder = folder;
        this.txt = txt;
        this.xml = xml;
    }

    public void run(Run<?, ?> run, TaskListener listener) throws IOException {
        InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(Constants.PS_UT);

        // Create a temporary file to store our powershell resource stream.
        File script = File.createTempFile("tdt-ut-", ".ps1");
        Files.copy(resourceStream, script.getAbsoluteFile().toPath(), REPLACE_EXISTING);

        // Create our command with appropriate arguments.
        String command = getProgram(script.getAbsolutePath())
                            .concat(getConnectionArgument())
                            .concat(getObjectsArgument())
                            .concat(getOutputPathArgument())
                            .concat(getReportFormatArguments());

        listener.getLogger().println(Constants.LOG_HEADER_UT + "Running unit test(s)...");

        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        process.getOutputStream().close();

        // Get output from the script.
        StreamThread outputStreamThread = new StreamThread(process.getInputStream(), run, listener, Constants.LOG_HEADER_UT);
        StreamThread errorStreamThread = new StreamThread(process.getErrorStream(), run, listener, Constants.LOG_HEADER_UT_ERR);

        outputStreamThread.start();
        errorStreamThread.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            listener.getLogger().println(Constants.LOG_HEADER_UT_ERR + writer.toString());
        }

        // Clean up our temp script once we're finished.
        if (!script.delete()) {
            script.deleteOnExit();
        }

        listener.getLogger().println(Constants.LOG_HEADER_UT + "Unit test(s) completed");
    }

    private String getProgram(String path) { return "powershell ".concat(path); }

    private String getConnectionArgument() {
        return " -connection ".concat(Base64.getEncoder().encodeToString(connection.getBytes()));
    }

    private String getObjectsArgument() {
        if (objects.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(",");
        for (UnitTestDBObject object : objects) {
            joiner.add(object.toString());
        }

        return " -objects ".concat(joiner.toString());
    }

    private String getOutputPathArgument() {
        return folder.isEmpty() ? "" : " -outputPath ".concat(Base64.getEncoder().encodeToString(folder.getBytes()));
    }

    private String getReportFormatArguments() {
        return (txt ? " -txt" : "").concat(xml ? " -xml" : "");
    }
}
