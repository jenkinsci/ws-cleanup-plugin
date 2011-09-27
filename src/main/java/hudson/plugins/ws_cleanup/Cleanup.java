package hudson.plugins.ws_cleanup;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;

class Cleanup  implements FileCallable<Object> {

    private final List<Pattern> patterns;

    public Cleanup(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    // Can't use FileCallable<Void> to return void
    public Object invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(f);
        String[] includes = new String[patterns.size()];
        int i = 0;
        for (Pattern pattern : patterns) {
            includes[i++] = pattern.getPattern();
        }
        ds.setIncludes(includes);
        ds.scan();
        for (String path : ds.getIncludedFiles()) {
            new File(f, path).delete();
        }
        return null;
    }
}