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
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UnitTestBuilder extends Builder implements SimpleBuildStep, Serializable {

    private String connection;
    private List<UnitTestDBObject> objects;
    private boolean txt;
    private boolean xml;

    public String getConnection() { return connection; }
    public List<UnitTestDBObject> getObjects() { return objects; }
    public boolean getTxt() { return txt; }
    public boolean getXml() { return xml; }

    @DataBoundConstructor
    public UnitTestBuilder(String connection, List<UnitTestDBObject> objects, boolean txt, boolean xml) {
        this.connection = connection;
        this.objects = objects == null ? new ArrayList<UnitTestDBObject>() : new ArrayList<UnitTestDBObject>(objects);
        this.txt = txt;
        this.xml = xml;
    }

    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

        // Expand any environment variables.
        EnvVars vars = run.getEnvironment(listener);
        String expConnection = vars.expand(connection);
        List<UnitTestDBObject> expObjects = expandObjects(vars, objects);

        run.setResult(launcher.getChannel().call(new MasterToSlaveCallable<Result, IOException>() {
            public Result call() throws IOException {

                UnitTestPowerShell script = new UnitTestPowerShell(expConnection, expObjects, txt, xml);
                return script.run(workspace, listener);
            }
        }));

    }

    private ArrayList<UnitTestDBObject> expandObjects(EnvVars vars, List<UnitTestDBObject> objects) {
        ArrayList<UnitTestDBObject> expObjects = new ArrayList<UnitTestDBObject>();
        for (UnitTestDBObject object : objects) {
            UnitTestDBObject expObject = new UnitTestDBObject(
                    vars.expand(object.getName()),
                    vars.expand(object.getOwner()));
            expObjects.add(expObject);
        }
        return expObjects;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckXml(@QueryParameter boolean value, @QueryParameter boolean txt) {
            return !value && !txt
                    ? FormValidation.warning(Messages.OutputFormatsEmpty()) : FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.UnitTestDisplayName();
        }
    }
}