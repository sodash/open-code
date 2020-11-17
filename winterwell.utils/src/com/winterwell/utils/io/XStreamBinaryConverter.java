package com.winterwell.utils.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.core.util.Base64Encoder;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;

/**
 * XStream does a good job with most classes, but it is lousy for e.g. matrices,
 * large arrays, images, etc. This class allows you to use a Java
 * {@link Serializable} binary encoding (converted into ascii via a base64
 * encoder). It is activated by the {@link BinaryXML} annotation.
 * 
 * <p>
 * Usage:
 * <p>
 * WebUtils.xstream.registerConverter(new
 * XStreamBinaryConverter(MyClass.class));
 * <p>
 * (all serialisation/deserialisation of this class will now use binary)
 * 
 * <p>
 * Legacy issues: binary converters are not compatible with non-binary
 * serialisations of objects. Sorry.
 * 
 * @warning Initialisation issues: if you fail to register the converter before
 *          deserialising, you may get a wrong object back, rather than an
 *          exception!
 * 
 * @author daniel
 * @testedby  XStreamBinaryConverterTest}
 */
public class XStreamBinaryConverter implements SingleValueConverter {

	/**
	 * Marks a class for using {@link XStreamBinaryConverter} and Java's
	 * serialisation when used with WebUtil's default XStream instance. This
	 * implies the class must implement {@link Serializable}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	// Meta-annotation for "Don't throw this away during compilation"
	@Target({ ElementType.TYPE })
	// Meta-annotation for "Only allowed on classes"
	public @interface BinaryXML {
	}

	static Base64Encoder enc = new Base64Encoder();

	static final Charset UTF8 = Charset.forName(StrUtils.ENCODING_UTF8);

	public XStreamBinaryConverter() {
		// Class<? extends Serializable> klass) {
		// this.klass = klass;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean canConvert(Class type) {
		java.lang.annotation.Annotation a = type.getAnnotation(BinaryXML.class);
		return a != null;
	}

	@Override
	public Object fromString(String str) {
		try {
			byte[] bytes = enc.decode(str);
			// convert the data from compressed bytes
			InputStream in = new FastByteArrayInputStream(bytes, bytes.length);
			in = new GZIPInputStream(in);
			ObjectInputStream objIn = new ObjectInputStream(in);
			Object data = objIn.readObject();
			objIn.close();
			return data;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * Use Java Serialisation & base 64 encoding to serialise.
	 * 
	 * @param obj
	 *            Must be {@link Serializable}
	 */
	@Override
	public String toString(Object obj) {
		if (obj == null)
			return null;
		try {
			StringBuffer w = new StringBuffer();
			// convert the data into compressed bytes
			FastByteArrayOutputStream out = new FastByteArrayOutputStream();
			GZIPOutputStream zipOut = new GZIPOutputStream(out);
			ObjectOutputStream objOut = new ObjectOutputStream(zipOut);
			objOut.writeObject(obj);
			objOut.close();
			// into the string
			byte[] bytes = out.getByteArrayCutToSize();
			String bs = enc.encode(bytes);
			w.append(bs);
			return w.toString();
		} catch (IOException e) {
			throw Utils.runtime(e);
		}
	}
}
