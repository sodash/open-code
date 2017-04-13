package com.winterwell.maths.matrix;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.core.util.Base64Encoder;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FastByteArrayInputStream;
import com.winterwell.utils.io.FastByteArrayOutputStream;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;


/**
 * DenseMatrix is not Serializable, so we need a specialist class.
 * 
 * @author daniel
 * 
 */
public class XStreamDenseMatrixConverter implements SingleValueConverter {

	static final Charset UTF8 = Charset.forName(StrUtils.ENCODING_UTF8);
	Base64Encoder enc = new Base64Encoder();

	@Override
	public boolean canConvert(Class type) {
		return type == DenseMatrix.class;
	}

	@Override
	public Object fromString(String str) {
		try {
			// header (could drop class name)
			int i1 = str.indexOf('|');
			String c = str.substring(0, i1);
			assert c.equals(DenseMatrix.class.getCanonicalName());
			i1++;
			int i2 = str.indexOf('|', i1);
			int rows = Integer.parseInt(str.substring(i1, i2));
			i2++;
			int i3 = str.indexOf('|', i2);
			int cols = Integer.parseInt(str.substring(i2, i3));
			i3++;
			// data
			String dstr = str.substring(i3);
			byte[] bytes = enc.decode(dstr);
			// convert the data from compressed bytes
			InputStream in = new FastByteArrayInputStream(bytes, bytes.length);
			in = new GZIPInputStream(in);
			ObjectInputStream objIn = new ObjectInputStream(in);
			Object data = objIn.readObject();
			objIn.close();
			double[] arr = (double[]) data;
			// make the matrix
			DenseVector vec = new DenseVector(arr, false);
			DenseMatrix m = new DenseMatrix(vec, false);
			ReflectionUtils.setPrivateField(m, "numRows", rows);
			ReflectionUtils.setPrivateField(m, "numColumns", cols);
			return m;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	@Override
	public String toString(Object obj) {
		if (obj == null)
			return null;
		try {
			DenseMatrix m = (DenseMatrix) obj;
			// header (could drop class name)
			String c = m.getClass().getCanonicalName();
			assert c != null;
			StringBuffer w = new StringBuffer();
			w.append(c);
			w.append('|');
			w.append(Integer.toString(m.numRows()));
			w.append('|');
			w.append(Integer.toString(m.numColumns()));
			w.append('|');
			double[] data = m.getData();
			// convert the data into compressed bytes
			FastByteArrayOutputStream out = new FastByteArrayOutputStream();
			GZIPOutputStream zipOut = new GZIPOutputStream(out);
			ObjectOutputStream objOut = new ObjectOutputStream(zipOut);
			objOut.writeObject(data);
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
