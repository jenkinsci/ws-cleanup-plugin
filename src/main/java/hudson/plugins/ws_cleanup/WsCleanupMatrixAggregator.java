package hudson.plugins.ws_cleanup;

import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WsCleanupMatrixAggregator extends MatrixAggregator {
	
	private final List<Pattern> patterns;
    private final boolean deleteDirs;
    private final boolean skipWhenFailed;
    private final boolean notFailBuild;
	
	public WsCleanupMatrixAggregator(MatrixBuild build, Launcher launcher, BuildListener listener, List<Pattern> patterns, 
			boolean deleteDirs, boolean skipWhenFailed, boolean notFailBuid) {
		super(build, launcher, listener);
		this.patterns = patterns;
		this.deleteDirs = deleteDirs;
		this.skipWhenFailed = skipWhenFailed;
		this.notFailBuild = notFailBuid;
    }
	
	public boolean endBuild() throws InterruptedException, IOException {
		return doWorkspaceCleanup();
    }
	
	private boolean doWorkspaceCleanup() throws IOException, InterruptedException {
		listener.getLogger().append("\nDeleting matrix project workspace... \n");
		
		//TODO do we want to keep keep child workpsaces if run on the same machine? Make it optional?
		/*
		VirtualChannel vch = build.getWorkspace().getChannel();
		String parentPath = build.getWorkspace().absolutize().toString();
		Set<String> filter = new HashSet<String>();
		for(MatrixRun run : build.getRuns()) {
			if(vch != run.getWorkspace().getChannel()) 
				continue;
			String childPath = run.getWorkspace().absolutize().toString();
			if(childPath.indexOf(parentPath) == 0) {
				if(!parentPath.endsWith(File.separator)) 
					parentPath = parentPath + File.separator;
				String relativePath = childPath.substring(parentPath.length());
				int firstDirIndex = relativePath.indexOf(File.separator);
				String childDir = relativePath.substring(0,firstDirIndex);
				//TODO add ./childDir ?
				filter.add(childDir);
			}
		}
		
		if(patterns == null) {
			patterns = new LinkedList<Pattern>();
		}
		for(String excludeDir : filter)
			patterns.add(new Pattern(excludeDir, PatternType.EXCLUDE));
		*/
		
        FilePath workspace = build.getWorkspace();        
        try {
        	if (workspace == null || !workspace.exists()) 
                return true;
        	if ( this.skipWhenFailed && build.getResult().isWorseOrEqualTo(Result.FAILURE)) {
        		listener.getLogger().append("skipped for failed build\n\n");
        		return true;
        	}
            if (patterns == null || patterns.isEmpty()) {
                workspace.deleteRecursive();
            } else {
                workspace.act(new Cleanup(patterns,deleteDirs));
            }
            listener.getLogger().append("done\n\n");
        } catch (Exception ex) {
            Logger.getLogger(WsCleanupMatrixAggregator.class.getName()).log(Level.SEVERE, null, ex);
            if(notFailBuild) {
            	listener.getLogger().append("Cannot delete workspace: " + ex.getCause() + "\n");
            	listener.getLogger().append("Option not to fail the build is turned on, so let's continue\n");
            	return true;
            }
            return false;
        }
        return true;
	}

}
