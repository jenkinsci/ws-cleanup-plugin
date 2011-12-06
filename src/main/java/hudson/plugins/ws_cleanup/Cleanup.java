package hudson.plugins.ws_cleanup;

import hudson.FilePath.FileCallable;
import hudson.Util;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;

class Cleanup  implements FileCallable<Object> {

    private final List<Pattern> patterns;
    private final boolean deleteDirs;

    public Cleanup(List<Pattern> patterns, boolean deleteDirs) {
        this.patterns = patterns;
        this.deleteDirs = deleteDirs;
    }

    public boolean getDeleteDirs(){
    	return deleteDirs;
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
        int length = ds.getIncludedFilesCount();
        if(deleteDirs)
        	length += ds.getIncludedDirsCount();
        String[] toDelete = new String[length]; 
        System.arraycopy(ds.getIncludedFiles(), 0, toDelete, 0, ds.getIncludedFilesCount());
        if(deleteDirs)
        	System.arraycopy(ds.getIncludedDirectories(), 0, toDelete, ds.getIncludedFilesCount(),ds.getIncludedDirsCount());
        for (String path :  toDelete) {
        	Util.deleteRecursive(new File(f, path));
        }
        return null;
    }
}