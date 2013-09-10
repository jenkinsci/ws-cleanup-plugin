package hudson.plugins.ws_cleanup;

import hudson.FilePath.FileCallable;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.plugins.ws_cleanup.Pattern.PatternType;
import hudson.remoting.VirtualChannel;
import hudson.slaves.EnvironmentVariablesNodeProperty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;

import org.apache.tools.ant.DirectoryScanner;

class Cleanup implements FileCallable<Object> {

    private List<Pattern> patterns;
    private final boolean deleteDirs;
    private String delete_command = null;
    private BuildListener listener = null;

    public Cleanup(List<Pattern> patterns, boolean deleteDirs, EnvironmentVariablesNodeProperty environment, String command, BuildListener listener) {

        this.deleteDirs = deleteDirs;
        try {
            this.delete_command = environment.getEnvVars().expand(command);            
            if(this.delete_command.length() == 0) {
                this.delete_command = null;
            }
        } catch( NullPointerException ex ) {
            this.delete_command = null;
        } 
        
        this.patterns = patterns;
        this.listener = listener;
        
        
    }

    public boolean getDeleteDirs() {
        return deleteDirs;
    }
    
    // Can't use FileCallable<Void> to return void
    public Object invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        String temp_command = null;
                
        if (delete_command != null && patterns == null) {            
            temp_command = delete_command.replaceAll("%s", "\"" + StringEscapeUtils.escapeJava(f.getPath()) + "\"");
            this.listener.getLogger().println("Using command: " + temp_command);
            Process deletion_proc = new ProcessBuilder(temp_command).start();
            deletion_proc.waitFor();
            
            return null;
        } else {
            if(patterns == null) {
                patterns = new ArrayList<Pattern>();
                patterns.add(new Pattern("**/*", PatternType.INCLUDE));
            }
            
            DirectoryScanner ds = new DirectoryScanner();
            ds.setBasedir(f);
            ArrayList<String> includes = new ArrayList<String>();
            ArrayList<String> excludes = new ArrayList<String>();
            for (Pattern pattern : patterns) {
                if (pattern.getType() == PatternType.INCLUDE) {
                    includes.add(pattern.getPattern());
                } else {
                    excludes.add(pattern.getPattern());
                }
            }
            //if there is no include pattern, set up ** (all) as include
            if (includes.isEmpty()) {
                includes.add("**/*");
            }
            String[] includesArray = new String[(includes.size())];
            String[] excludesArray = new String[excludes.size()];
            includes.toArray(includesArray);
            excludes.toArray(excludesArray);
            ds.setIncludes(includesArray);
            ds.setExcludes(excludesArray);
            ds.scan();
            int length = ds.getIncludedFilesCount();
            if (deleteDirs) {
                length += ds.getIncludedDirsCount();
            }
            String[] toDelete = new String[length];
            System.arraycopy(ds.getIncludedFiles(), 0, toDelete, 0, ds.getIncludedFilesCount());
            if (deleteDirs) {
                System.arraycopy(ds.getIncludedDirectories(), 0, toDelete, ds.getIncludedFilesCount(), ds.getIncludedDirsCount());
            }
            for (String path : toDelete) {
                if (delete_command != null) {
                    temp_command = delete_command.replaceAll("%s", "\"" + StringEscapeUtils.escapeJava((new File(f, path)).getPath()) + "\"");
                    this.listener.getLogger().println("Using command: " + temp_command);
                    Process deletion_proc = new ProcessBuilder(temp_command).start();
                    deletion_proc.waitFor();
                } else {
                    Util.deleteRecursive(new File(f, path));
                }
            }
            return null;
        }
    }
}