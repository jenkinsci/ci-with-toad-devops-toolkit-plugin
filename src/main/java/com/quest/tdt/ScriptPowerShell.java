package com.quest.tdt;

import hudson.FilePath;
import hudson.model.Result;
import hudson.model.TaskListener;
import com.quest.tdt.util.StreamThread;
import com.quest.tdt.util.Constants;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ScriptPowerShell implements Serializable {
    private String connection;
    private boolean sourcetype;
    private String filePathOrScript;
    private String filePath;
    private String OutputName;
    private String OutputFilePath;
    private int maxRows;

    protected ScriptPowerShell(String connection, boolean sourcetype, String filePathOrScript, String OutputName, int maxRows) {
        this.connection = connection;
        this.sourcetype = sourcetype;
        this.filePathOrScript = filePathOrScript;
        this.OutputName = OutputName;
        this.maxRows = maxRows;
    }

    public Result run(final FilePath workspace, TaskListener listener) throws IOException {
        InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(Constants.PS_S);

        // Create a temporary directory to hold scripts and reports
        Path tempdirectory = Files.createTempDirectory("tdt-s-");

        // Create a temporary file to store our powershell resource stream.
        File script = File.createTempFile("tdt-s-", ".ps1", new File(tempdirectory.toString()));
        Files.copy(resourceStream, script.getAbsoluteFile().toPath(), REPLACE_EXISTING);

        // Is source type is script, create temporary file.
        filePath = filePathOrScript;
        if (sourcetype) {
            filePath = createTempScriptFile(tempdirectory.toString(), filePathOrScript);
        }

        // Set the output file, if output name is supplied
        if (!OutputName.isEmpty()) {
            File OutputFile = new File(tempdirectory.toString(), OutputName+".out");
            OutputFilePath = OutputFile.getAbsolutePath();
        }
        else {
            OutputFilePath = "";
        }

        // Create our command with appropriate arguments.
        String command = getProgram(script.getAbsolutePath())
                .concat(getConnectionArgument())
                .concat(getMaxRowsArgument())
                .concat(getFilePathArgument())
                .concat(getOutputNameArgument());

        listener.getLogger().println(Constants.LOG_HEADER_S + "Executing script...");

        Result result = Result.SUCCESS;
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        process.getOutputStream().close();

        StreamThread outputStreamThread = new StreamThread(process.getInputStream(), listener, Constants.LOG_HEADER_S, result);
        StreamThread errorStreamThread = new StreamThread(process.getErrorStream(), listener, Constants.LOG_HEADER_S_ERR, result);

        outputStreamThread.start();
        errorStreamThread.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            listener.getLogger().println(Constants.LOG_HEADER_S_ERR + writer.toString());

            result = Result.ABORTED;
        }

        // See if we received any errors during stream processing
        if (outputStreamThread.getResult()!=Result.SUCCESS)
            result = outputStreamThread.getResult();
        else if (errorStreamThread.getResult()!=Result.SUCCESS)
            result = errorStreamThread.getResult();

        listener.getLogger().println(Constants.LOG_HEADER_S + "Script execution completed");

        // Now copy the generated output to the workspace
        try {

            FilePath outputpath = new FilePath(new File(tempdirectory.toString()));
            outputpath.copyRecursiveTo("*.out", workspace);

        } catch (InterruptedException e) {

            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            listener.getLogger().println(Constants.LOG_HEADER_S_ERR + writer.toString());
        }

        // Delete the temporary directory and all contents
        Files.walk(tempdirectory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);

        // return the result of the unit tests
        return result;
    }

    private String createTempScriptFile(String basedirectory, String content) throws IOException {
        File file = File.createTempFile("tdt-", ".sql", new File(basedirectory));
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file.getAbsolutePath()), StandardCharsets.UTF_8);
        try {
            writer.write(content);
        } finally {
            writer.close();
        }
        return  file.getAbsolutePath();
    }

    private String getProgram(String path) { return "powershell ".concat(path); }

    private String getConnectionArgument() {
        return " -connection ".concat(Base64.getEncoder().encodeToString(connection.getBytes(StandardCharsets.UTF_8)));
    }

    private String getMaxRowsArgument() {
        return " -maxRows ".concat(Integer.toString(maxRows));
    }

    private String getFilePathArgument() {
        return " -inputFile ".concat(Base64.getEncoder().encodeToString(filePath.getBytes(StandardCharsets.UTF_8)));
    }

    private String getOutputNameArgument() {
        return OutputFilePath.isEmpty() ? "" : " -outputFile ".concat(Base64.getEncoder().encodeToString(OutputFilePath.getBytes(StandardCharsets.UTF_8)));
    }

}
