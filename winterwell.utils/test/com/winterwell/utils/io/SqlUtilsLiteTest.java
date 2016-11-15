package winterwell.utils.io;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.winterwell.utils.Printer;

import com.winterwell.utils.containers.Pair;

public class SqlUtilsLiteTest {

	@Test
	public void testDebugInfo() {
		int[] info = SqlUtils.getPostgresThreadInfo("sodash");
		Printer.out(info);
	}
	


}
