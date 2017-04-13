package com.winterwell.maths.chart;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.junit.Test;

import com.winterwell.utils.gui.GuiUtils;

public class RainbowTest  {

	@Test
	public void test2Colors() {
		Rainbow r = new Rainbow(2);
		BufferedImage img = new BufferedImage(300, 100,
				BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		for (int i = 0; i < r.size(); i++) {
			g.setColor(r.getColor(i));
			g.fillRect(i * 50, 0, 50, 100);
		}
		g.dispose();
		GuiUtils.popupAndBlock(img);		
	}

	@Test
	public void testGetColor() {
		Rainbow r = new Rainbow(6);
		BufferedImage img = new BufferedImage(300, 100,
				BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		for (int i = 0; i < 6; i++) {
			g.setColor(r.getColor(i));
			g.fillRect(i * 50, 0, 50, 100);
		}
		g.dispose();
		GuiUtils.popupAndBlock(img);
	}

	@Test
	public void testGetDarkColor() {
		Rainbow r = new Rainbow(6, new Color(128, 0, 0));
		BufferedImage img = new BufferedImage(300, 100,
				BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		for (int i = 0; i < 6; i++) {
			g.setColor(r.getColor(i));
			g.fillRect(i * 50, 0, 50, 100);
		}
		g.dispose();
		GuiUtils.popupAndBlock(img);		
	}

}
