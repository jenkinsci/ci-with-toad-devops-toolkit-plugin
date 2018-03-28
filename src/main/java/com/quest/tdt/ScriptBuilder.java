package com.quest.tdt;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ScriptBuilder extends Builder implements SimpleBuildStep {
    private String connection;
    private String script;
    private String file;
    // Used for determining between the checked radio blocks: script, file.
    private String sourceType;
    private int maxRows;
    private String output;

    @DataBoundConstructor
    public ScriptBuilder(String connection, String script, String file, String sourceType, int maxRows, String output) {
        this.connection = connection;
        this.script = script;
        this.file = file;
        this.sourceType = sourceType;
        this.maxRows = maxRows;
        this.output = output;
    }

    public String getConnection() { return connection; }
    public String getScript() { return sourceType.equals("script") ? script : ""; }
    public String getFile() { return sourceType.equals("file") ? file : ""; }
    public String getSourceType() { return sourceType; };
    public int getMaxRows() { return maxRows; };
    public String getOutput() { return output; };

    public String isSourceType(String sourceType) { return this.sourceType.equals(sourceType) ? "true" : ""; }

    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        String filePath = file;

        File tempFile = null;
        boolean isSourceTypeScript = isSourceType("script").equals("true");

        if (isSourceTypeScript) {
            tempFile = createTempScriptFile(script);
            filePath = tempFile.getAbsolutePath();
        }

        // Expand any environment variables.
        EnvVars vars = run.getEnvironment(listener);

        String expConnection = vars.expand(connection);
        String expFilePath = vars.expand(filePath);
        String expOutput = vars.expand(output);

        listener.getLogger().println(output);
        listener.getLogger().println(vars.expand(output));

        ScriptPowerShell script = new ScriptPowerShell(expConnection, expFilePath, maxRows, expOutput);
        script.run(listener);

        if (isSourceTypeScript) {
            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }

    private File createTempScriptFile(String content) throws IOException {
        File file = File.createTempFile("tdt-", ".sql");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file.getAbsolutePath()));
        try {
            bufferedWriter.write(script);
        } finally {
            bufferedWriter.close();
        }
        return  file;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckConnection(@QueryParameter String value) {
            return value.isEmpty() ? FormValidation.error(Messages.ConnectionEmpty()) : FormValidation.ok();
        }

        public FormValidation doCheckScript(@QueryParameter String value) {
            return value.isEmpty() ? FormValidation.error(Messages.ScriptEmpty()) : FormValidation.ok();
        }

        public FormValidation doCheckFile(@QueryParameter String value) {
            return value.isEmpty() ? FormValidation.error(Messages.FilePathEmpty()) : FormValidation.ok();
        }

        public FormValidation doCheckOutput(@QueryParameter String value) {
            return value.isEmpty()
                    ? FormValidation.warning(Messages.FilePathOutputEmpty()) : FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.ScriptDisplayName();
        }
    }
}
