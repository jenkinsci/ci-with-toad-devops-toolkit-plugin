package com.quest.tdt.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import hudson.model.Result;
import hudson.model.TaskListener;

public class StreamThread extends Thread {
    private InputStream inputStream;
    private TaskListener listener;
    private String type;
    private Result result;

    public Result getResult() { return result; };

    public StreamThread(InputStream inputStream, TaskListener listener, String type, Result result) {
        this.inputStream = inputStream;
        this.listener = listener;
        this.type = type;
        this.result = result;
    }

    public void run() {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // Failure is a keyword that denotes that we should fail the build step.
                if (line.equals(Constants.FAILURE)) {
                    result = Result.FAILURE;
                } else {
                    listener.getLogger().println(type + line);

                    // If this is processing the error stream, then set result to failure
                    if (type.contains("Error")) {
                        result = Result.FAILURE;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
