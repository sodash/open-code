/**
 * 
 */
package com.winterwell.utils.gui;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;

/**
 * Wrapper for Images which allows them to be serialised using a specified file
 * format. Usage: When saving-to-file is not an option. E.g. in a database
 * persistence system.
 * 
 * @author Daniel Winterstein
 */
public final class SerializableImage implements Serializable {

	/*
	 * Serializable form is: format name (a UTF8 string) image data size (an
	 * int) image data (a byte array)
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The format for saving, e.g. "gif"
	 */
	private String formatName;

	private Image img;

	/**
	 * @param img
	 *            The image to store.
	 * @param formatName
	 *            The format to use for storage. E.g. "gif"
	 */
	public SerializableImage(Image img, String formatName) {
		assert img != null && formatName != null;
		this.img = img;
		this.formatName = formatName;
	}

	/**
	 * The underlying Image object.
	 */
	public Image getImage() {
		return img;
	}

	private void readObject(ObjectInputStream in) throws IOException {
		// Get the format
		formatName = in.readUTF();
		// Get the image data
		int size = in.readInt();
		byte[] bytes = new byte[size];
		in.readFully(bytes);
		// Make an image
		img = ImageIO.read(new ByteArrayInputStream(bytes));
		if (img == null)
			throw new IOException("Could not load image");
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		assert img != null && formatName != null;
		// Store the format
		out.writeUTF(formatName);
		// Get data
		// ByteArrayOutputStream is not ideal since it requires a copy to
		// access the data. You might consider using a custom implementation
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		GuiUtils.saveImageToStream(img, formatName, stream);
		byte[] bytes = stream.toByteArray();
		// Store the data
		out.writeInt(bytes.length);
		out.write(bytes);
	}

}
