package com.quest.tdt;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.security.MasterToSlaveCallable;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.*;

public class ScriptBuilder extends Builder implements SimpleBuildStep, Serializable {
    private String connection;
    private String script;
    private String file;
    // Used for determining between the checked radio blocks: script, file.
    private String sourceType;
    private String outputName;
    private int maxRows;

    @DataBoundConstructor
    public ScriptBuilder(String connection, String script, String file, String sourceType, String outputName, String limitMaxRows, int maxRows) {
        this.connection = connection;
        this.script = script;
        this.file = file;
        this.sourceType = sourceType;
        this.outputName = outputName;
        this.maxRows = maxRows;

        if (!limitMaxRows.equals("true")) {
            this.maxRows = 0;
        }
    }

    public String getConnection() { return connection; }
    public String getScript() { return sourceType.equals("script") ? script : ""; }
    public String getFile() { return sourceType.equals("file") ? file : ""; }
    public String getSourceType() { return sourceType; };
    public String getOutputName() { return outputName; };
    public int getMaxRows() { return maxRows; };

    public String isSourceType(String sourceType) { return this.sourceType.equals(sourceType) ? "true" : ""; }
    public String limitMaxRows() { return this.maxRows > 0 ? "true" : ""; };

    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

        String PathOrSource = file;
        boolean isSourceTypeScript = isSourceType("script").equals("true");
        if (isSourceTypeScript) {
            PathOrSource = script;
        }

        // Expand any environment variables.
        EnvVars vars = run.getEnvironment(listener);
        String expConnection = vars.expand(connection);
        String expPathOrSource = vars.expand(PathOrSource);
        String expName = vars.expand(outputName);

        run.setResult(launcher.getChannel().call(new MasterToSlaveCallable<Result, IOException>() {
            public Result call() throws IOException {

                ScriptPowerShell script = new ScriptPowerShell(expConnection, isSourceTypeScript, expPathOrSource, expName, maxRows);
                return script.run(workspace, listener);
            }
        }));

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

        public FormValidation doCheckOutputName(@QueryParameter String value) {
            return value.isEmpty()
                    ? FormValidation.warning("Name must not be empty to receive output.") : FormValidation.ok();
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
