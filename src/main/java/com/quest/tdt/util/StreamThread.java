package com.quest.tdt.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import hudson.model.TaskListener;

public class StreamThread extends Thread {
    private InputStream inputStream;
    private TaskListener listener;
    private String type;

    public StreamThread(InputStream inputStream, TaskListener listener, String type) {
        this.inputStream = inputStream;
        this.listener = listener;
        this.type = type;
    }

    public void run() {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                listener.getLogger().println(type + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
