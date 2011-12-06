package hudson.plugins.ws_cleanup;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.ListBoxModel;

import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class Pattern implements Serializable, Describable<Pattern>{

    private final String pattern;
    private PatternType type;

    @DataBoundConstructor
    public Pattern(String pattern, PatternType type) {
        this.pattern = pattern;
        this.type = type;
    }

    public Object readResolve(){
    	if(type == null)
    		type = PatternType.INCLUDE;
    	return this;
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public PatternType getType(){
    	return type;
    }
    
	public Descriptor<Pattern> getDescriptor() {
		return Hudson.getInstance().getDescriptor(getClass());
	}
	
	@Extension
    public static final class DescriptorImpl extends Descriptor<Pattern>{

    	public String getDisplayName(){
    		return "Directory scanner pattern";
    	}
    	
    	public ListBoxModel doFillTypeItems(){
    		ListBoxModel model = new ListBoxModel(2);
    		model.add("Include",PatternType.INCLUDE.toString());
    		model.add("Exclude",PatternType.EXCLUDE.toString());
    		return model;
    	}

	}
    
    public enum PatternType{
    	INCLUDE,
    	EXCLUDE;
    }
    
    private static final long serialVerisonUID = 1L;
}
