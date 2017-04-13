package com.winterwell.maths;

public interface IFactory<In, X> {

	/**
	 * Creates a new object of the same class <b>and configuration</b> as the
	 * invocant. This is not a deep copy: data-streams, internal state variables
	 * and settings learnt from training are not copied. Neither is it a clone:
	 * the "basic source" for data may be changed.
	 * <p>
	 * <b>Be careful when implementing this that you do copy all the
	 * settings!</b> Not just the constructor ones, but also the setXXX() ones.
	 * <p>
	 * Used with pipeline classes such as IDataStream and ITokenStream If
	 * appropriate, this should recursively call factory() on the base stream in
	 * order to recreate the pipeline. E.g. <code><pre>
// recurse
IDataStream source = base.isFactory()?
				base.factory(sourceSpecifier)
				: (IDataStream) sourceSpecifier;
// clone
clone = new MyStream(source);
clone.setConfigStuff(myConfig);
return clone;
		</pre></code>
	 * 
	 * @param sourceSpecifier
	 *            This depends on the processing chain - it is whatever the end
	 *            of the chain expects! *Either* this is an I(Data|Token)Stream
	 *            to be used, *or* it provides the information for the
	 *            lowest-level stream to create a new data stream (e.g. it might
	 *            specify a source file).
	 * 
	 * @return the new data stream object.
	 * @throws ClassCastException
	 *             if sourceSpecifier was not of the right type.
	 * 
	 *             <p>
	 *             Note: I tried to add a generic type to sourceSpecifier, but
	 *             this becomes ugly as data streams which wrap other data
	 *             streams have trouble with this.
	 *             <p>
	 *             Note: Why not clone() then setSource()? Because setSource()
	 *             is bug-prone - the internal state needs to be properly reset
	 *             - plus it prevents the use of final variables which are nice
	 *             to have.
	 */
	X factory(In sourceSpecifier);

	boolean isFactory();
}
