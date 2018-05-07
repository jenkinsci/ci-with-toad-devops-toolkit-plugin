package com.quest.tdt;

import com.quest.tdt.util.Constants;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CodeAnalysisDBObject extends AbstractDescribableImpl<CodeAnalysisDBObject> {
    private String name;
    private String owner;
    private String type;

    public String getName() { return name; }

    public String getOwner() { return owner; }

    public String getType() { return type; }

    @DataBoundConstructor
    public CodeAnalysisDBObject(String name, String owner, String type) {
        super();

        this.name = name;
        this.owner = owner;
        this.type = type;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<CodeAnalysisDBObject> {

        public FormValidation doCheckOwner(@QueryParameter String value) {
            return value.isEmpty() ? FormValidation.error(Messages.OwnerEmpty()) : FormValidation.ok();
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            return value.isEmpty() ? FormValidation.error(Messages.NameEmpty()) : FormValidation.ok();
        }

        public ListBoxModel doFillTypeItems() {
            ListBoxModel items = new ListBoxModel();
            items.add(Messages.DBObjectTypeAllTypes(), "");
            items.add(Messages.DBObjectTypeFunctions(), "FUNCTION");
            items.add(Messages.DBObjectTypeProcedures(), "PROCEDURE");
            items.add(Messages.DBObjectTypeTriggers(), "TRIGGER");
            items.add(Messages.DBObjectTypeViews(), "VIEW");
            items.add(Messages.DBObjectTypePackages(), "PACKAGE");

            return items;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.DBObjectDisplayName();
        }
    }

    /**
     * Returns a period '.' delimited base64 encoded string representation of the object in the order of "type.owner.name".
     * @return a string representation of the object.
     */
    public String toString() {
        String encodedName = Base64.getEncoder().encodeToString(getName().getBytes(StandardCharsets.UTF_8));
        String encodedOwner = Base64.getEncoder().encodeToString(getOwner().getBytes(StandardCharsets.UTF_8));
        String encodedType = Base64.getEncoder().encodeToString(getType().getBytes(StandardCharsets.UTF_8));

        return String.format("%s.%s.%s", encodedType, encodedOwner, encodedName);
    }
}
