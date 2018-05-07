package com.quest.tdt;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class UnitTestDBObject extends AbstractDescribableImpl<UnitTestDBObject> {
    private String name;
    private String owner;

    public String getName() { return name; }

    public String getOwner() { return owner; }

    @DataBoundConstructor
    public UnitTestDBObject(String name, String owner) {
        super();

        this.name = name;
        this.owner = owner;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<UnitTestDBObject> {

        public FormValidation doCheckOwner(@QueryParameter String value) {
            return value.isEmpty() ? FormValidation.error(Messages.OwnerEmpty()) : FormValidation.ok();
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            return value.isEmpty() ? FormValidation.error(Messages.NameEmpty()) : FormValidation.ok();
        }

        @Nonnull
        @Override
        public String getDisplayName() { return Messages.DBObjectDisplayName(); }
    }

    /**
     * Returns a period '.' delimited base64 encoded string representation of the object in the order of "type.owner.name".
     * @return a string representation of the object.
     */
    public String toString() {
        String encodedOwner = Base64.getEncoder().encodeToString(getOwner().getBytes(StandardCharsets.UTF_8));
        String encodedName = Base64.getEncoder().encodeToString(getName().getBytes(StandardCharsets.UTF_8));

        return String.format("%s.%s", encodedOwner, encodedName);
    }
}
