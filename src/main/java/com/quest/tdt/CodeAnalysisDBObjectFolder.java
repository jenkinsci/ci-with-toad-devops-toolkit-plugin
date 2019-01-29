package com.quest.tdt;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.io.Serializable;
import java.util.Base64;

public class CodeAnalysisDBObjectFolder extends AbstractDescribableImpl<CodeAnalysisDBObjectFolder> implements Serializable {
    private String path;
    private String filter;
    private boolean recurse;

    public String getPath() { return path; }
    public String getFilter() { return filter; }
    public boolean getRecurse() { return recurse; }

    @DataBoundConstructor
    public CodeAnalysisDBObjectFolder(String path, String filter, boolean recurse) {
        super();

        this.path = path;
        this.filter = filter;
        this.recurse = recurse;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<CodeAnalysisDBObjectFolder> {

        public FormValidation doCheckPath(@QueryParameter String value) {
            return value.isEmpty() ? FormValidation.error(Messages.DirectoryEmpty()) : FormValidation.ok();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.DBObjectFolderDisplayName();
        }
    }

    /**
     * Returns a period '.' delimited base64 encoded string representation of the folder in the order of "path.filter.recurse".
     * @return a string representation of the object.
     */
    public String toString() {
        String encodedPath = Base64.getEncoder().encodeToString(getPath().getBytes(StandardCharsets.UTF_8));
        String encodedFilter = Base64.getEncoder().encodeToString(getFilter().getBytes(StandardCharsets.UTF_8));
        String encodedRecurse = Base64.getEncoder().encodeToString(Boolean.toString(getRecurse()).getBytes(StandardCharsets.UTF_8));

        return String.format("%s.%s.%s", encodedPath, encodedFilter, encodedRecurse);
    }
}