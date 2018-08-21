/**
 *
 */
package com.winterwell.utils.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Mutable;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;

class BlockingImageObserver implements ImageObserver {

	private boolean block = true;

	public synchronized void block() {
		while (block) {
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
	

	@Override
	public synchronized boolean imageUpdate(Image img, int infoflags, int x,
			int y, int width, int height) {
		block = false;
		notifyAll();
		return (infoflags & (ALLBITS | ABORT)) == 0;
	}

}

/**
 * For standard dialogs (apart from file selection), see {@link JOptionPane}
 * 
 * @author daniel
 * 
 */
public class GuiUtils {

	public static interface EventHandler {

		void onClick(JComponent clicked);

	}


	public static void createThumbnailImage(BufferedImage image, File thumbFile) {
		// Make thumbnail
		BufferedImage thumbnail = scaleWidthHeight(image, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
		GuiUtils.saveImage(thumbnail, thumbFile);
	}


	/**
	 * what's a sensible arbitrary?
	 * https://en.wikipedia.org/wiki/Thumbnail#Dimensions
	 */
	private static final int THUMBNAIL_WIDTH = 150;
	private static final int THUMBNAIL_HEIGHT = 150;

	public static BufferedImage scaleWidthHeight(BufferedImage image, double maxWidth, double maxHeight) {
		double scale = Math.max(image.getWidth()/maxWidth, image.getHeight()/maxHeight);
		int w = (int) (image.getWidth()/scale);
		int h = (int) (image.getHeight()/scale);
		w = Math.max(w, 1);
		h = Math.max(h, 1);
		// ?? image was all black! this is going to be a jpg - so no alpha channel
		image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		GuiUtils.scaleImage(image, image);
		return image;
	}


	/** Winterwell's less corporate blue **/
	public static Color WINTERWELL_BLUE = getColor("#0096bb");

	/** Winterwell's corporate pink **/
	public static Color WINTERWELL_PINK = getColor("#e14184");

	/**
	 * Mnenomic for {@link JOptionPane#showInputDialog(Object)}
	 * 
	 * @param question
	 * @return
	 */
	public static String askUser(String question) {		
		return JOptionPane.showInputDialog(question);
	}

	/**
	 * Mnenomic for {@link JOptionPane#showInputDialog(Object)}
	 * 
	 * @param question
	 * @return
	 */
	public static <X> X askUserSelect(String title, String text, Collection<X> options) {		
		int optionType = JOptionPane.OK_CANCEL_OPTION;
		int messageType = JOptionPane.QUESTION_MESSAGE;
		Icon icon = null; 
		Object initialValue = null;
		Component parentComponent = null;
		Object[] aoptions = options.toArray();
		int i = JOptionPane.showOptionDialog(parentComponent, text, title, optionType, messageType, icon, aoptions, initialValue);
		return (X) aoptions[i].toString();
	}
	
	// .showOptionDialog(null, options, question, 0, 0, null, options, options);
	// JDialog dialog = new JDialog();
	// dialog.setTitle(question);
	// Box box = Box.createVerticalBox();
	// box.add(new JLabel(question));
	// Box optionsBox = Box.createHorizontalBox();
	// for (final String string : options) {
	// JButton button = new JButton(string);
	// button.setAction(new AbstractAction(){
	// @Override
	// public void actionPerformed(ActionEvent e) {
	// answer = string;
	// }
	// });
	// }
	// box.add(optionsBox);
	// dialog.add(box);
	// dialog.setVisible(true);
	// return null;
	// }

	/**
	 * Block this thread whilst the given frame is open. Useful in interactive
	 * tests for holding open a display. If {@link GuiUtils#isInteractive()} is
	 * false, this will return immediately.
	 * 
	 * @param frame
	 */
	public static void blockWhileOpen(JFrame frame) {
		if (!GuiUtils.isInteractive())
			return;
		while (frame.isVisible()) {
			Utils.sleep(200);
		}
	}

	/**
	 * Mnenomic for {@link JOptionPane#showConfirmDialog(Component, Object)}
	 * 
	 * @param question
	 * @return true if OK
	 */
	public static boolean confirm(String msg) {
		int opt = JOptionPane.showConfirmDialog(null, msg);
		return opt == JOptionPane.OK_OPTION;
	}
	/**
	 * Mnenomic for {@link JOptionPane#showConfirmDialog(Component, Object)}
	 * 
	 * @param question
	 * @return true if OK
	 */
	public static void alert(String msg) {
		if ( ! isInteractive()) {
			Log.e("alert", msg);
			return;
		}
		int messageType = JOptionPane.WARNING_MESSAGE;
		JOptionPane.showMessageDialog(null, msg, "Alert: "+StrUtils.ellipsize(msg, 80), 
							messageType);
	}
	
	public static BufferedImage copy(Image img) {
		int type = img instanceof BufferedImage ? ((BufferedImage) img)
				.getType() : BufferedImage.TYPE_INT_ARGB;
		BufferedImage copy = new BufferedImage(getWidth(img), getHeight(img),
				type);
		Graphics g = copy.getGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		return copy;
	}

	/**
	 * 
	 * @param fade
	 *            [0,1] 0=a, 1=b
	 * @param a
	 * @param b
	 * @return a colour partway between a and b
	 * 
	 * @testedby {@link GuiUtilsTest#testFade()}
	 */
	public static Color fade(double fade, Color a, Color b) {
		assert a != null && b != null;
		assert MathUtils.isProb(fade) : fade;
		if (fade == 0)
			return a;
		if (fade == 1)
			return b;
		int[] argba = new int[] { a.getRed(), a.getGreen(), a.getBlue(),
				a.getAlpha() };
		int[] brgba = new int[] { b.getRed(), b.getGreen(), b.getBlue(),
				b.getAlpha() };
		int[] rgba = new int[4];
		for (int i = 0; i < 4; i++) {
			rgba[i] = (int) Math.round((1 - fade) * argba[i] + fade * brgba[i]);
		}
		return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
	}

	public static Rectangle getBoundingRectangle(Image img) {
		int w = getWidth(img);
		int h = getHeight(img);
		return new Rectangle(w, h);
	}

	/**
	 * Get a color from a web-style RGB hex string e.g. #ffff00. The string may
	 * optionally begin with a '#' character.
	 * 
	 * @see WebUtils#color2html(Color)
	 * @testedby {@link GuiUtilsTest#testHexColor}
	 */
	public static Color getColor(String hex) {
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		}
		return new Color(Integer.parseInt(hex, 16));
	}

	private static int getHeight(Image img) {
		int v = img.getHeight(null);
		if (v != -1)
			return v;
		// handle the case where the image is still being loaded
		BlockingImageObserver observer = new BlockingImageObserver();
		img.getHeight(observer);
		observer.block();
		return getHeight(img);
	}

	public static BufferedImage getImage(JComponent panel) {
		assert panel.getWidth() != 0 && panel.getHeight() != 0;
		panel.doLayout(); // This seems necessary to draw without displaying
							// anything
		BufferedImage img = new BufferedImage(panel.getWidth(),
				panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		panel.paint(g);
		g.dispose();
		return img;
	}
		

	/**
	 * 
	 * @param whiteness
	 *            [0,1], 0 indicates black, 1 white
	 * @return a randomly chosen colour of roughly the given level of
	 *         light/darkness
	 */
	public static Color getRandomColour(double whiteness) {
		assert MathUtils.isProb(whiteness) : whiteness;
		// This is not perfect but what the hell
		Random rnd = Utils.getRandom();
		float r = rnd.nextFloat();
		float g = rnd.nextFloat();
		float b = rnd.nextFloat();
		float sum = r + g + b;
		float s = 3 / sum;
		return new Color(Math.min(r / s, 1), Math.min(g / s, 1), Math.min(
				b / s, 1));
	}

	private static int getWidth(Image img) {
		int v = img.getWidth(null);
		if (v != -1)
			return v;
		// handle the case where the image is still being loaded
		BlockingImageObserver observer = new BlockingImageObserver();
		img.getWidth(observer);
		observer.block();
		return img.getHeight(null);
	}

	/**
	 * @return true if there is a user you can ask for input. true by
	 *         default.
	 *         <p>
	 *         This is set by the environment variable WINTERWELL_HEADLESS, with
	 *         WINTERWELL_HEADLESS=true meaning interactive is false. See
	 *         http://blog.plover.com/prog/sh-flags.html
	 *         <p>
	 *         If not set, it uses {@link GraphicsEnvironment#isHeadless()}
	 */
	public static boolean isInteractive() {
		String envVar = System.getProperty("WINTERWELL_HEADLESS");
		if (envVar == null)
			return !GraphicsEnvironment.isHeadless();
		return Utils.yes(envVar);
	}
	
	public static void setInteractive(boolean interactive) {
		System.setProperty("WINTERWELL_HEADLESS", ""+interactive);
	}

	public static BufferedImage load(File imgFile) throws WrappedException {
		assert imgFile.exists() : imgFile.getPath()
				+ " does not exist! absolute-path=" + imgFile.getAbsolutePath();
		try {
			return ImageIO.read(imgFile);
		} catch (IOException e) {
			throw new WrappedException("Cause: " + imgFile, e);
		}
	}

	/**
	 * Because I find Swing's action setup confusing. So this tries to be more
	 * like JavaScript
	 * 
	 * @param button
	 * @param eventHandler
	 * @return
	 */
	public static JButton onClick(final JButton button,
			final EventHandler eventHandler) {
		button.setAction(new AbstractAction() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				eventHandler.onClick(button);
			}
		});
		return button;
	}

	public static JFrame popup(Component component, String title) {
		JFrame frame = new JFrame(title);
		frame.add(component);
		frame.pack();
		frame.validate();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		return frame;
	}
	
	public static JFrame popup(Image img, String title) {
		return popup(img, title, null);
	}

	/**
	 * Display in a popup frame.
	 * 
	 * @param img
	 * @param title
	 * @return the frame, which should now be visible
	 */
	public static JFrame popup(Image img, String title, Mutable.Ref<ImageIcon> imgIcon) {
		assert img != null;
		JFrame frame = new JFrame(title);
		ImageIcon icon = new ImageIcon(img);
		if (imgIcon!=null) imgIcon.value = icon;
		frame.add(new JLabel(icon));
		frame.pack();
		frame.validate();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		return frame;
	}

	public static void popupAndBlock(Image img) {
		if ( ! isInteractive()) return;
		JFrame f = popup(img, "");
		blockWhileOpen(f);
	}

	/**
	 * Save a JComponent (e.g. a JFreeChart) as an image.
	 * 
	 * @param panel
	 *            the panle to save
	 * @param file
	 *            This will determine the format (e.g. "myfile.gif" will result
	 *            in a gif file)
	 * @throws IORException
	 *             If something goes wrong
	 * @see #saveImage(Image, File) which this method wraps
	 */
	public static void saveAsImage(JComponent panel, File file) {
		BufferedImage img = getImage(panel);
		saveImage(img, file);
	}

	/**
	 * 
	 * @param image
	 *            The image to save
	 * @param file
	 *            This will determine the format (e.g. "myfile.gif" will result
	 *            in a gif file)
	 * @throws IORException
	 *             If something goes wrong
	 * @see #saveImageToStream(Image, String, OutputStream) which this method
	 *      wraps
	 */
	public static void saveImage(Image image, File file)
			throws WrappedException {
		try {
			String formatName = FileUtils.getType(file);
			FileOutputStream fout = new FileOutputStream(file);
			saveImageToStream(image, formatName, fout);
			// FileUtils.close(fout);
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * Save an image to an OutputStream
	 * 
	 * @param img
	 * @param formatName
	 *            E.g. "gif"
	 * @param stream
	 *            Save to this stream and close it
	 * @see #saveImage(Image, File)
	 */
	public static void saveImageToStream(Image img, String formatName,
			OutputStream stream) {
		try {
			// Get a RenderedImage
			RenderedImage rImg;
			if (img instanceof RenderedImage) {
				rImg = (RenderedImage) img;
			} else {
				BufferedImage bImg = new BufferedImage(getWidth(img),
						getHeight(img), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = bImg.createGraphics();
				boolean ok = g.drawImage(img, 0, 0, null);
				if (!ok) { // handle the case where the image is still being
							// loaded
					BlockingImageObserver observer = new BlockingImageObserver();
					g.drawImage(img, 0, 0, observer);
					observer.block();
					g.drawImage(img, 0, 0, null); // ??Why is this necessary?
				}
				g.dispose();
				rImg = bImg;
			}
			// Write and close
			ImageIO.write(rImg, formatName, stream);
			FileUtils.close(stream);
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * TODO how is scaling done? is it smart or dumb?
	 * 
	 * @param img
	 * @param out
	 *            Draw over this
	 */
	public static void scaleImage(Image img, BufferedImage out) {
		Graphics2D g = out.createGraphics();
		int width = out.getWidth();
		int height = out.getHeight();
		BlockingImageObserver observer = new BlockingImageObserver();
		boolean ok = g.drawImage(img, 0, 0, width, height, observer);
		if (!ok) { // handle the case where the image is still being loaded
			observer.block();
		}
		g.dispose();
	}

	/**
	 * Create a copy of an image, scaled to the given size.
	 * 
	 * @param img
	 * @param width
	 * @param height
	 * @return
	 */
	public static BufferedImage scaleImage(Image img, int width, int height) {
		assert width > 0 && height > 0 && img != null;
		int type = BufferedImage.TYPE_INT_ARGB;
		if (img instanceof BufferedImage) {
			type = ((BufferedImage) img).getType();
			if (type == BufferedImage.TYPE_CUSTOM) {
				type = BufferedImage.TYPE_INT_ARGB;
			}
		}
		BufferedImage bImg = new BufferedImage(width, height, type);
		scaleImage(img, bImg);
		return bImg;
	}

	public static File selectFile(String msg, File dir) {
		return selectFile(msg, dir, null);
	}
	/**
	 * Pick a file. Note: This must be a file, not a directory.
	 * @param msg
	 * @param dir
	 *            The starting directory. null for the user's directory
	 * @return selected file or null
	 */
	public static File selectFile(String msg, File dir, FileFilter filterOrNull) {
		if ( ! isInteractive()) {
			return null;
		}
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(dir);
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		// fc.setAcceptAllFileFilterUsed(true);
		// if (regexFilter!=null)
		// fc.setFileFilter(FileUtils.getRegexFilter(regexFilter));
		int ok = fc.showDialog(null, msg);
		if (ok == JFileChooser.APPROVE_OPTION) {
			File f = fc.getSelectedFile();
			if (f==null) return null;
			if (filterOrNull!=null && ! filterOrNull.accept(f)) {
				// The Java file-picker makes it easy to choose a directory by mistake
				return selectFile(msg, f, filterOrNull);			
			}
			return f;
		}
		return null;
	}

}
