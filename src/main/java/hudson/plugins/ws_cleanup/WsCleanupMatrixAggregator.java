package hudson.plugins.ws_cleanup;

import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.Result;
import hudson.slaves.EnvironmentVariablesNodeProperty;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WsCleanupMatrixAggregator extends MatrixAggregator {
	
	private final List<Pattern> patterns;
    private final boolean deleteDirs;
    private final boolean notFailBuild;

    private final boolean cleanWhenSuccess;
    private final boolean cleanWhenUnstable;
    private final boolean cleanWhenFailure;
    private final boolean cleanWhenNotBuilt;
    private final boolean cleanWhenAborted;
    private final String externalDelete;
	
	public WsCleanupMatrixAggregator(MatrixBuild build, Launcher launcher, BuildListener listener, List<Pattern> patterns, 
			boolean deleteDirs, boolean cleanWhenSuccess, boolean cleanWhenUnstable, boolean cleanWhenFailure,
			boolean cleanWhenNotBuilt, boolean cleanWhenAborted, boolean notFailBuild, String externalDelete) {
		super(build, launcher, listener);
		this.patterns = patterns;
		this.deleteDirs = deleteDirs;
        this.cleanWhenSuccess = cleanWhenSuccess;
        this.cleanWhenUnstable = cleanWhenUnstable;
        this.cleanWhenFailure = cleanWhenFailure;
        this.cleanWhenNotBuilt = cleanWhenNotBuilt;
        this.cleanWhenAborted = cleanWhenAborted;
		this.notFailBuild = notFailBuild;
        this.externalDelete = externalDelete;
    }
	
    @Override
    public boolean endBuild() throws InterruptedException, IOException {
		return doWorkspaceCleanup();
    }

    private boolean shouldCleanBuildBasedOnState(@CheckForNull Result result) {
        if (result == null) {
            return true;
        }
        if(result.equals(Result.SUCCESS))
            return this.cleanWhenSuccess;
        if(result.equals(Result.UNSTABLE))
            return this.cleanWhenUnstable;
        if(result.equals(Result.FAILURE))
            return this.cleanWhenFailure;
        if(result.equals(Result.NOT_BUILT))
            return this.cleanWhenNotBuilt;
        if(result.equals(Result.ABORTED))
            return this.cleanWhenAborted;

        return true;
    }

	private boolean doWorkspaceCleanup() throws IOException, InterruptedException {
		listener.getLogger().println("Deleting matrix project workspace...");
		
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
            if(!shouldCleanBuildBasedOnState(build.getResult())) {
                listener.getLogger().println("Skipped based on build state " + build.getResult() + "\n");
                return true;
            }
            if (patterns == null || patterns.isEmpty()) {
                workspace.deleteRecursive();
            } else {
                Node node = build.getBuiltOn();
                if (node != null) {
                    workspace.act(new Cleanup(patterns,deleteDirs, node.getNodeProperties().get(
                            EnvironmentVariablesNodeProperty.class), externalDelete, listener));
                }
            }
            listener.getLogger().append("done\n\n");
        } catch (Exception ex) {
            Logger.getLogger(WsCleanupMatrixAggregator.class.getName()).log(Level.SEVERE, null, ex);
            if(notFailBuild) {
            	listener.getLogger().println("Cannot delete workspace: " + ex.getCause());
            	listener.getLogger().println("Option not to fail the build is turned on, so let's continue");
            	return true;
            }
            return false;
        }
        return true;
	}
}
