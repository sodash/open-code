package com.winterwell.depot;

import java.io.File;

import org.junit.Test;

import com.winterwell.utils.Utils;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.FileUtils;

public class DoARemotePutTest {

	@Test
	public void testUploadFile() {
		Depot depot = Depot.getDefault();
		File artifact = GuiUtils.selectFile("Pick a file", FileUtils.getWorkingDirectory());
		Desc<File> desc = new Desc<File>(artifact.getName(), File.class);
		String tag = GuiUtils.askUser("What tag?");
		assert ! Utils.isBlank(tag);
		desc.setTag(tag);
		depot.put(desc, artifact);
	}
	
}
