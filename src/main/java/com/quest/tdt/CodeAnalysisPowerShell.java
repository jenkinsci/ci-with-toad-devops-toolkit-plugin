package com.quest.tdt;

import hudson.model.TaskListener;

import com.quest.tdt.util.StreamThread;
import com.quest.tdt.util.Constants;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Base64;
import java.util.StringJoiner;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class CodeAnalysisPowerShell {
    private String connection;
    private List<CodeAnalysisDBObject> objects;
    private List<CodeAnalysisDBObjectFolder> objectFolders;
    private int ruleSet;
    private CodeAnalysisReport report;

    public CodeAnalysisPowerShell(String connection, List<CodeAnalysisDBObject> objects, List<CodeAnalysisDBObjectFolder> objectFolders, int ruleSet, CodeAnalysisReport report) {
        this.connection = connection;
        this.objects = objects;
        this.objectFolders = objectFolders;
        this.ruleSet = ruleSet;
        this.report = report;
    }

    public void run(TaskListener listener) throws IOException {
        InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(Constants.PS_CA);

        // Create a temporary file to store our powershell resource stream.
        File script = File.createTempFile("tdt-ca-", ".ps1");
        Files.copy(resourceStream, script.getAbsoluteFile().toPath(), REPLACE_EXISTING);

        // Create our command with appropriate arguments.
        String command = getProgram(script.getAbsolutePath())
                .concat(getConnectionArgument())
                .concat(getObjectsArgument())
                .concat(getObjectFoldersArgument())
                .concat(getRuleSetArgument())
                .concat(getReportNameArgument())
                .concat(getReportFolder())
                .concat(getReportFormats());

        listener.getLogger().println(Constants.LOG_HEADER_CA + "Performing analysis...");

        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        process.getOutputStream().close();

        StreamThread outputStreamThread = new StreamThread(process.getInputStream(), listener, Constants.LOG_HEADER_CA);
        StreamThread errorStreamThread = new StreamThread(process.getErrorStream(), listener, Constants.LOG_HEADER_CA_ERR);

        outputStreamThread.start();
        errorStreamThread.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            listener.getLogger().println(Constants.LOG_HEADER_CA_ERR + writer.toString());
        }

        // Clean up our temp script once we're finished.
        if (!script.delete()) {
            script.deleteOnExit();
        }

        listener.getLogger().println(Constants.LOG_HEADER_CA + "Analysis completed");
    }

    private String getProgram(String path) {
        return "powershell ".concat(path);
    }

    private String getConnectionArgument() {
        return " -connection ".concat(Base64.getEncoder().encodeToString(connection.getBytes()));
    }

    private String getObjectsArgument() {
        if (objects.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(",");
        for (CodeAnalysisDBObject object : objects) {
            joiner.add(object.toString());
        }

        return " -objects ".concat(joiner.toString());
    }

    private String getObjectFoldersArgument() {
        if (objectFolders.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(",");
        for (CodeAnalysisDBObjectFolder objectFolder : objectFolders) {
            joiner.add(objectFolder.toString());
        }

        return " -folders ".concat(joiner.toString());
    }

    private String getRuleSetArgument() {
        return " -ruleSet ".concat(Integer.toString(ruleSet));
    }

    private String getReportNameArgument() {
        return report.getName().isEmpty()
                ? "" : " -reportName ".concat(Base64.getEncoder().encodeToString(report.getName().getBytes()));
    }

    private String getReportFolder() {
        return report.getFolder().isEmpty()
                ? "" : " -reportFolder ".concat(Base64.getEncoder().encodeToString(report.getFolder().getBytes()));
    }

    private String getReportFormats() {
        return (report.getHtml() ? " -html" : "")
                .concat(report.getJson() ? " -json" : "")
                .concat(report.getXls() ? " -xls" : "")
                .concat(report.getXml() ? " -xml" : "");
    }
}
