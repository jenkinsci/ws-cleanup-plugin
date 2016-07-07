package hudson.plugins.ws_cleanup;

import hudson.FilePath.FileCallable;
import hudson.FilePath;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.plugins.ws_cleanup.Pattern.PatternType;
import hudson.remoting.VirtualChannel;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.tools.ant.DirectoryScanner;
import org.jenkinsci.remoting.RoleChecker;

/**
 * Perform configured cleanup on remote directory.
 */
class Cleanup extends RemoteCleaner implements FileCallable<Object> {

    private List<Pattern> patterns;
    private final boolean deleteDirs;
    private String deleteCommand = null;
    private BuildListener listener = null;

    public Cleanup(List<Pattern> patterns, boolean deleteDirs, EnvironmentVariablesNodeProperty environment,
            String command, BuildListener listener) {

        this.deleteDirs = deleteDirs;
        this.listener = listener;
        this.patterns = (patterns == null) ? Collections.<Pattern>emptyList() : patterns;
        this.deleteCommand = (command == null || command.isEmpty()) ? null : command;

        if (environment != null && deleteCommand != null) { // allow slave environment to overwrite delete cmd
            this.deleteCommand = environment.getEnvVars().get(command);
        }

        if (patterns == null) { // if pattern is not set up, delete everything
            patterns = new ArrayList<Pattern>();
            patterns.add(new Pattern("**/*", PatternType.INCLUDE));
        }
    }

    public boolean getDeleteDirs() {
        return deleteDirs;
    }

    // Can't use FileCallable<Void> to return void
    public Object invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setFollowSymlinks(false);
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
        // if there is no include pattern, set up ** (all) as include
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
            System.arraycopy(ds.getIncludedDirectories(), 0, toDelete, ds.getIncludedFilesCount(),
                    ds.getIncludedDirsCount());
        }

        for (String path : toDelete) {
            if (deleteCommand != null) {
                List<String> cmdList = fixQuotesAndExpand((new File(f, path)).getPath());
                doDelete(cmdList);
            } else {
                Util.deleteRecursive(new File(f, path));
            }
        }
        
        //not followed symlinks are returned as absolute paths, needs to be removed separately
        final String[] nonFollowedSymlinks = ds.getNotFollowedSymlinks();
        for (String link : nonFollowedSymlinks) {
            if (deleteCommand != null) {
                List<String> cmdList = fixQuotesAndExpand((new File(link)).getPath());
                doDelete(cmdList);
            } else {
                Util.deleteRecursive(new File(link));
            }
        }
        
        return null;
    }

    /**
     * 
     * THB I don't remember what exactly original author meant in 998354608 (and why I merge it), but my understanding
     * is that it should support windows tool, which can contain spaces in path to external tool as well as in paths to
     * be deleted. If command or path contains spaces, not to split it, whole piece is quoted. It should also support
     * some strange parameter order in form of $cmd %s /parameters.
     * 
     */
    private List<String> fixQuotesAndExpand(String fullPath) {
        String tempCommand = null;
        if (deleteCommand.contains("%s")) {
            tempCommand = deleteCommand.replaceAll("%s", "\"" + Matcher.quoteReplacement(fullPath) + "\"");
        } else {
            tempCommand = deleteCommand + " " + fullPath;
        }
        List<String> cmdList = new ArrayList<String>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"([^\"]+)\"|(\\S+)");
        java.util.regex.Matcher m = p.matcher(tempCommand);
        while (m.find()) {
            if (m.group(1) != null) {
                cmdList.add(m.group(1));
            }
            if (m.group(2) != null) {
                cmdList.add(m.group(2));
            }
        }
        return cmdList;
    }

    private void doDelete(List<String> cmdList) throws IOException, InterruptedException {
        Process deletProc = new ProcessBuilder(cmdList).start();
        int exit = deletProc.waitFor();
        if (exit != 0) {
            listener.error("Cleanup command '%s' failed with code %d:", Util.join(cmdList, " "), exit);
            InputStream err = deletProc.getErrorStream();
            try {
                Util.copyStream(err, listener.getLogger());
            } finally {
                IOUtils.closeQuietly(err);
            }
        }
    }

    @Override
    protected void perform(FilePath workspace) throws IOException, InterruptedException {
        workspace.act(this);
    }

    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {

    }
}
