/**
 * 
 */
package winterwell.optimization.genetic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.junit.Test;

import winterwell.optimization.AEvaluate;
import winterwell.optimization.IEvaluate;
import winterwell.optimization.genetic.GA.Generation;
import winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import winterwell.utils.web.XStreamUtils;

/**
 * @author Joe Halliwell <joe@winterwell.com>
 *
 */
public class GATest {

	/**
	 * Fairly convoluted functional test that does the standard learn
	 * maximum value in unary thing
	 * @throws IOException 
	 */
	@Test
	public void testBasic() throws IOException {
		GA<String> ga = getUnaryMaxGA();
		IEvaluate<String,Double> objective1 = getUnaryMaxObjective();
		
		String best1 = ga.optimize(objective1);
		assert best1.equals("11111");
	}

	@Test
	public void testSave() throws IOException {
		GA<String> ga = getUnaryMaxGA();
		IEvaluate<String,Double> objective1 = getUnaryMaxObjective();
		
		File intFile = new File("test/test_ga.xml");
		FileUtils.delete(intFile);
		File backup = new File("test/test_ga.xml.old");
		ga.setIntermediateFile(intFile);
		
		String best1 = ga.optimize(objective1);
		
		assertEquals("Optimization succeeded", "11111", best1);
		assertTrue("Intermediate file created", intFile.exists());		
		assert ! backup.exists();
		Generation<String> generation = XStreamUtils.serialiseFromXml(FileUtils.read(intFile));
		assertNotNull("Intermediate file can be read cleanly", generation);				
	}
	
	@Test
	public void testLoad() throws IOException {
		GA<String> ga = getUnaryMaxGA();
		IEvaluate<String,Double> objective1 = getUnaryMaxObjective();
		
		File saved = new File("test/test_ga_49.xml");
		ga.setIntermediateFile(saved);
		String best1 = ga.optimize(objective1);
		assertEquals("Optimization succeeded", "11111", best1);
	}

	private IEvaluate<String,Double> getUnaryMaxObjective() {
		IEvaluate<String,Double> objective1 = new AEvaluate<String>() {
			@Override
			public double evaluate(String candidate) {
				assert candidate.length() == 5;
				double score = 0;
				for (int i = 0; i < 5; i++) {
					if (candidate.charAt(i) == '1')
						score = score + 1;
				}
				return score;
			}
		};
		return objective1;
	}
	
	private GA<String> getUnaryMaxGA() {
		IBreeder<String> generator = new IBreeder<String>() {
			@Override
			public String generate() {
				return "00000";
			}
			Random r=Utils.getRandom();
			@Override
			public String mutate(String candidate) {
				int mutation = r.nextInt(5);
				char c = candidate.charAt(mutation);
				return candidate.substring(0, mutation)
					+ (c == '0' ? '1' : '0')
					+ candidate.substring(mutation + 1);
			}
			@Override
			public String crossover(String a, String b) {
				assert a.length() == 5;
				assert b.length() == 5;
				int xpoint = r.nextInt(3) + 1;
				String c = a.substring(0, xpoint) + b.substring(xpoint);
				assert c.length() == 5;
				return c;
			}
			@Override
			public void setRandomSource(Random seed) {
				r = seed;
			}
		};
		GA<String> ga = new GA<String>(100, generator);
		return ga;
	}	
}
