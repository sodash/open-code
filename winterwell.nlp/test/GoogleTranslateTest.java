import org.junit.Test;

import com.winterwell.nlp.languages.GoogleTranslate;

public class GoogleTranslateTest  {
	
	@Test
	public void testAccountIsNotCurrentlyActive() {
		try {
			GoogleTranslate gt = new GoogleTranslate("en", "de");
			String german = gt.translate("Hello world");
			System.out.println(german);
			assert false;
		} catch(Exception ex) {
			// as expected
		}
	}
	
}
