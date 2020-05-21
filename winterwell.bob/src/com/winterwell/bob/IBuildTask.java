package com.winterwell.bob;

import java.io.Closeable;
import java.util.List;

import com.winterwell.depot.IHasDesc;

/**
 * TODO a lighter weight interface for implementing build scripts 
 * @author daniel
 *
 */
public interface IBuildTask extends Closeable, IHasDesc {



	/**
	 * @return The build tasks this task depends on. Dependencies are run
	 *         first, and will be checked to avoid repeats. 
	 */
	List<BuildTask> getDependencies();
}
