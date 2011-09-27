package hudson.plugins.ws_cleanup;

import java.io.Serializable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class Pattern implements Serializable{

    private final String pattern;

    @DataBoundConstructor
    public Pattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }
    
    private static final long serialVerisonUID = 1L;
}
