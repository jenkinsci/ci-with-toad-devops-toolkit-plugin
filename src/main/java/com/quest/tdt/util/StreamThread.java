package com.quest.tdt.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

public class StreamThread extends Thread {
    private InputStream inputStream;
    private Run<?, ?> run;
    private TaskListener listener;
    private String type;

    public StreamThread(InputStream inputStream, Run<?, ?> run, TaskListener listener, String type) {
        this.inputStream = inputStream;
        this.run = run;
        this.listener = listener;
        this.type = type;
    }

    public void run() {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // Failure is a keyword that denotes that we should fail the build step.
                if (line.equals(Constants.FAILURE)) {
                    run.setResult(Result.FAILURE);
                } else {
                    listener.getLogger().println(type + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
