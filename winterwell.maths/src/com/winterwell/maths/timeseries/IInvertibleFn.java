package com.winterwell.maths.timeseries;

import com.winterwell.utils.IFn;

public interface IInvertibleFn<In, Out> extends IFn<In, Out> {

	IFn<Out, In> inverse();
}
