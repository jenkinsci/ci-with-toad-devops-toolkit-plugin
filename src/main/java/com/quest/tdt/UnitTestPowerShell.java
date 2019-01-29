package com.quest.tdt;

import com.quest.tdt.util.Constants;
import com.quest.tdt.util.StreamThread;
import hudson.FilePath;
import hudson.model.Result;
import hudson.model.TaskListener;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class UnitTestPowerShell implements Serializable{
    private String connection;
    private List<UnitTestDBObject> objects;
    private boolean txt;
    private boolean xml;

    public UnitTestPowerShell(String connection, List<UnitTestDBObject> objects, boolean txt, boolean xml) {
        this.connection = connection;
        this.objects = objects;
        this.txt = txt;
        this.xml = xml;
    }

    public Result run(final FilePath workspace, TaskListener listener) throws IOException {
        InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(Constants.PS_UT);

        // Create a temporary directory to hold scripts and reports
        Path tempdirectory = Files.createTempDirectory("tdt-ut-");

        // Create a temporary file to store our powershell resource stream.
        File script = File.createTempFile("tdt-ut-", ".ps1", new File(tempdirectory.toString()));
        Files.copy(resourceStream, script.getAbsoluteFile().toPath(), REPLACE_EXISTING);

        // Create our command with appropriate arguments.
        String command = getProgram(script.getAbsolutePath())
                            .concat(getConnectionArgument())
                            .concat(getObjectsArgument())
                            .concat(" -outputPath ".concat(Base64.getEncoder().encodeToString(tempdirectory.toString().getBytes(StandardCharsets.UTF_8))))
                            .concat(getReportFormatArguments());

        listener.getLogger().println(Constants.LOG_HEADER_UT + "Running unit test(s)...");

        Result result = Result.SUCCESS;
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        process.getOutputStream().close();

        // Get output from the script.
        StreamThread outputStreamThread = new StreamThread(process.getInputStream(), listener, Constants.LOG_HEADER_UT, result);
        StreamThread errorStreamThread = new StreamThread(process.getErrorStream(), listener, Constants.LOG_HEADER_UT_ERR, result);

        outputStreamThread.start();
        errorStreamThread.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            listener.getLogger().println(Constants.LOG_HEADER_UT_ERR + writer.toString());

            result = Result.ABORTED;
        }

        // See if we received any errors during stream processing
        if (outputStreamThread.getResult()!=Result.SUCCESS)
            result = outputStreamThread.getResult();
        else if (errorStreamThread.getResult()!=Result.SUCCESS)
            result = errorStreamThread.getResult();

        listener.getLogger().println(Constants.LOG_HEADER_UT + "Unit test(s) completed");

        // Now copy the generated reports to the workspace
        try {

            FilePath reportpath = new FilePath(new File(tempdirectory.toString()));
            reportpath.copyRecursiveTo("*.xml,*.txt", workspace);

        } catch (InterruptedException e) {

            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            listener.getLogger().println(Constants.LOG_HEADER_CA_ERR + writer.toString());
        }

        // Delete the temporary directory and all contents
        Files.walk(tempdirectory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);

        // return the result of the unit tests
        return result;
    }

    private String getProgram(String path) { return "powershell ".concat(path); }

    private String getConnectionArgument() {
        return " -connection ".concat(Base64.getEncoder().encodeToString(connection.getBytes(StandardCharsets.UTF_8)));
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

     private String getReportFormatArguments() {
        return (txt ? " -txt" : "").concat(xml ? " -xml" : "");
    }
}
