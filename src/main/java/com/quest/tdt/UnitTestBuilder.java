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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UnitTestBuilder extends Builder implements SimpleBuildStep {

    private String connection;
    private List<UnitTestDBObject> objects;
    private String folder;
    private boolean txt;
    private boolean xml;

    public String getConnection() { return connection; }
    public List<UnitTestDBObject> getObjects() { return objects; }
    public String getFolder() { return folder; }
    public boolean getTxt() { return txt; }
    public boolean getXml() { return xml; }

    @DataBoundConstructor
    public UnitTestBuilder(String connection, List<UnitTestDBObject> objects, String folder, boolean txt, boolean xml) {
        this.connection = connection;
        this.objects = objects == null ? new ArrayList<UnitTestDBObject>() : new ArrayList<UnitTestDBObject>(objects);
        this.folder = folder;
        this.txt = txt;
        this.xml = xml;
    }

    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        EnvVars vars = run.getEnvironment(listener);
        String expConnection = vars.expand(connection);
        List<UnitTestDBObject> expObjects = expandObjects(vars, objects);
        String expFolder = vars.expand(folder);

        UnitTestPowerShell script = new UnitTestPowerShell(expConnection, expObjects, expFolder, txt, xml);
        script.run(listener);
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

        public FormValidation doCheckFolder(@QueryParameter String value) {
            return value.isEmpty()
                    ? FormValidation.warning(Messages.OutputDirectoryEmpty()) : FormValidation.ok();
        }

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