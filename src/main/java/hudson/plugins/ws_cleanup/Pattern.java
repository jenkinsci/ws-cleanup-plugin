package hudson.plugins.ws_cleanup;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import java.io.Serializable;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class Pattern implements Serializable, Describable<Pattern> {
    private static final long serialVersionUID = 5405352019347262189L;

    private final String pattern;
    private PatternType type;

    @DataBoundConstructor
    public Pattern(String pattern, PatternType type) {
        this.pattern = pattern;
        this.type = type;
    }

    public Object readResolve() {
        if (type == null) {
            type = PatternType.INCLUDE;
        }
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public PatternType getType() {
        return type;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.get().getDescriptorOrDie(getClass());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Pattern> {

        private static final ListBoxModel TYPES = new ListBoxModel(
                new ListBoxModel.Option("Include", PatternType.INCLUDE.toString()),
                new ListBoxModel.Option("Exclude", PatternType.EXCLUDE.toString()));

        @NonNull
        @Override
        public String getDisplayName() {
            return "Directory scanner pattern";
        }

        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillTypeItems() {
            return TYPES;
        }
    }

    public enum PatternType {
        INCLUDE,
        EXCLUDE
    }
}
