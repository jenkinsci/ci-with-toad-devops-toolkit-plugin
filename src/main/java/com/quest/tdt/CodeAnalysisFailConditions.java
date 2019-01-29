package com.quest.tdt;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.Serializable;


public class CodeAnalysisFailConditions extends AbstractDescribableImpl<CodeAnalysisFailConditions> implements Serializable {
    private int halstead;
    private int maintainability;
    private int mcCabe;
    private int TCR;
    private boolean ruleViolations;
    private boolean syntaxErrors;
    private boolean ignoreWrappedPackages;

    @DataBoundConstructor
    public CodeAnalysisFailConditions(int halstead, int maintainability, int mcCabe, int TCR, boolean ruleViolations,
                                      boolean syntaxErrors, boolean ignoreWrappedPackages) {
        this.halstead = halstead;
        this.maintainability = maintainability;
        this.mcCabe = mcCabe;
        this.TCR = TCR;
        this.ruleViolations = ruleViolations;
        this.syntaxErrors = syntaxErrors;
        this.ignoreWrappedPackages = ignoreWrappedPackages;
    }

    public int getHalstead() {
        return halstead;
    }

    public int getMaintainability() {
        return maintainability;
    }

    public int getMcCabe() {
        return mcCabe;
    }

    public int getTCR() {
        return TCR;
    }

    public boolean getRuleViolations() {
        return ruleViolations;
    }

    public boolean getSyntaxErrors() {
        return syntaxErrors;
    }

    public boolean getIgnoreWrappedPackages() {
        return ignoreWrappedPackages;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<CodeAnalysisFailConditions> {

        public ListBoxModel doFillHalsteadItems() {
            ListBoxModel items = new ListBoxModel();
            items.add(Messages.FailConditionsHalstead(), "0");
            items.add(Messages.FailConditionsHalsteadReasonable(), "1");
            items.add(Messages.FailConditionsHalsteadChallenging(), "2");
            items.add(Messages.FailConditionsHalsteadTooComplex(), "3");
            return items;
        }

        public ListBoxModel doFillMaintainabilityItems() {
            ListBoxModel items = new ListBoxModel();
            items.add(Messages.FailConditionsMaintainability(), "0");
            items.add(Messages.FailConditionsMaintainabilityHighly(), "1");
            items.add(Messages.FailConditionsMaintainabilityModerate(), "2");
            items.add(Messages.FailConditionsMaintainabilityDifficult(), "3");
            return items;
        }

        public ListBoxModel doFillMcCabeItems() {
            ListBoxModel items = new ListBoxModel();
            items.add(Messages.FailConditionsMcCabe(), "0");
            items.add(Messages.FailConditionsMcCabeSmallRisk(), "1");
            items.add(Messages.FailConditionsMcCabeModerateRisk(), "2");
            items.add(Messages.FailConditionsMcCabeHighRisk(), "3");
            items.add(Messages.FailConditionsMcCabeVeryHighRisk(), "4");
            return items;
        }

        public ListBoxModel doFillTCRItems() {
            ListBoxModel items = new ListBoxModel();
            items.add(Messages.FailConditionsTCR(), "0");
            items.add(Messages.FailConditionsTCRGood(), "1");
            items.add(Messages.FailConditionsTCROK(), "2");
            items.add(Messages.FailConditionsTCRFair(), "3");
            items.add(Messages.FailConditionsTCRPoor(), "4");
            return items;
        }

        public FormValidation doCheckIgnoreWrappedPackages(@QueryParameter boolean value,
                                                           @QueryParameter int halstead,
                                                           @QueryParameter int maintainability,
                                                           @QueryParameter int mcCabe,
                                                           @QueryParameter int TCR,
                                                           @QueryParameter boolean ruleViolations,
                                                           @QueryParameter boolean syntaxErrors) {
            return !value && !ruleViolations && !syntaxErrors
                    && halstead == 0 && maintainability == 0 && mcCabe == 0 && TCR == 0
                        ? FormValidation.warning("One or more fail conditions should be set to receive failures.") : FormValidation.ok();
        }

        @Nonnull
        @Override
        public String getDisplayName() { return "Code Analysis Fail Conditions"; }
    }
}
