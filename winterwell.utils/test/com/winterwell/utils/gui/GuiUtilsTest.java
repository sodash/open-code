package winterwell.utils.gui;

import java.awt.Color;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.gui.GuiUtils;

import junit.framework.TestCase;
import winterwell.utils.web.WebUtils;

public class GuiUtilsTest extends TestCase {


	public void testAlert() {
		GuiUtils.alert("Hello (warning)");
	}
	
	public void testFade() {
		{
			Color rg = GuiUtils.fade(0.5, Color.RED, Color.GREEN);
			assert rg.getRed() == 128 : rg.getRed();
			assert rg.getGreen() == 128 : rg.getGreen();
			assert rg.getBlue() == 0 : rg.getBlue();
			assert rg.getAlpha() == 255 : rg.getAlpha();
		}
		{
			Color gb = GuiUtils.fade(0.75, Color.GREEN, Color.BLUE);
			assert gb.getRed() == 0 : gb.getRed();
			assert MathUtils.approx(gb.getGreen(), 0.25 * 255) : gb.getGreen();
			assert MathUtils.approx(gb.getBlue(), 0.75 * 255) : gb.getBlue();
			assert gb.getAlpha() == 255 : gb.getAlpha();
		}
	}

	public void testHexColor() {
		{
			Color col = GuiUtils.getColor("#9fcdf0");
			Color col2 = GuiUtils.fade(0.35, col, Color.WHITE);
			Printer.out(WebUtils.color2html(col2));
		}
	}

}
