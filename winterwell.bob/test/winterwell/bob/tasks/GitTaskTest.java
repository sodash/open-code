package winterwell.bob.tasks;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;

public class GitTaskTest {

	@Test
	public void testGetGitBranch() {
		String branch = GitTask.getGitBranch(null);
		System.out.println(branch);
		
		File sodashDir = new File(FileUtils.getWinterwellDir(), "sodash");
		String branch2 = GitTask.getGitBranch(sodashDir);
		System.out.println(branch2);
	}

	@Test
	public void testGetLastCommitId() {
		String hash = GitTask.getLastCommitId(null);
		System.out.println(hash);
		
		File sodashDir = new File(FileUtils.getWinterwellDir(), "sodash");
		String hash2 = GitTask.getLastCommitId(sodashDir);
		System.out.println(hash2);
	}



	@Test
	public void testGetLastCommitInfo() {
		Map<String, Object> info = GitTask.getLastCommitInfo(null);
		System.out.println(info);		
	}

	
}
