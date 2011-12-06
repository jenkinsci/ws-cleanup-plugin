package hudson.plugins.ws_cleanup;

import hudson.FilePath.FileCallable;
import hudson.Util;
import hudson.plugins.ws_cleanup.Pattern.PatternType;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
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
        ArrayList<String> includes = new ArrayList<String>();
        ArrayList<String> excludes = new ArrayList<String>();
        for (Pattern pattern : patterns) {
        	if(pattern.getType() == PatternType.INCLUDE)
        		includes.add(pattern.getPattern());
        	else
        		excludes.add(pattern.getPattern());
        }	
        //if there is no include pattern, set up ** (all) as include
        if(includes.size() == 0)
        	includes.add("**/*");
        String[] includesArray = new String[(includes.size())];
        String[] excludesArray = new String[excludes.size()]; 
        includes.toArray(includesArray);
        excludes.toArray(excludesArray);
        ds.setIncludes(includesArray);
        ds.setExcludes(excludesArray);
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