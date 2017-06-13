import java.io.File;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;

import jobs.BuildWinterwellProject;

public class BuildWWAppBase extends BuildWinterwellProject {

	public BuildWWAppBase() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/winterwell.webappbase"));
	}

}
