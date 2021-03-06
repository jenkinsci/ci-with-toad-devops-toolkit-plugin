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

public class CodeAnalysisReport extends AbstractDescribableImpl<CodeAnalysisReport> implements Serializable {
    private String name;
    private boolean html;
    private boolean json;
    private boolean xls;
    private boolean xml;

    @DataBoundConstructor
    public CodeAnalysisReport(String name, boolean html, boolean json, boolean xls, boolean xml) {
        this.name = name;
        this.html = html;
        this.json = json;
        this.xls = xls;
        this.xml = xml;
    }

    public String getName() { return name; }
    public boolean getHtml() { return html; }
    public boolean getJson() { return json; }
    public boolean getXls() { return xls; }
    public boolean getXml() { return xml; }

    @Extension
    public static class DescriptorImpl extends Descriptor<CodeAnalysisReport> {

        public FormValidation doCheckName(@QueryParameter String value) {
            return value.isEmpty()
                    ? FormValidation.warning("Name must not be empty to receive report(s).") : FormValidation.ok();
        }

        public FormValidation doCheckXml(@QueryParameter boolean value, @QueryParameter boolean html,
                                         @QueryParameter boolean json, @QueryParameter boolean xls) {
            return !value && !html && !json && !xls
                    ? FormValidation.warning("One or more formats must be checked to receive report(s).") : FormValidation.ok();
        }

        @Nonnull
        @Override
        public String getDisplayName() { return "Code Analysis Report"; }
    }

    /**
     * Returns a period '.' delimited base64 encoded string representation of the object in the order of "folder.name.html".
     * @return a string representation of the object.
     */
    public String toString() {

        String encodedName = Base64.getEncoder().encodeToString(getName().getBytes(StandardCharsets.UTF_8));
        return encodedName;
    }
}
