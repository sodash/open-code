package com.winterwell.maths.chart;

public class Scaling {

	public static enum KScaling {
		EXPONENTIAL, GAMMA, LINEAR, LOG
	}

	public static final Scaling EXP = new Scaling(KScaling.EXPONENTIAL, 1);

	public double param = 1;

	public final KScaling type;

	public Scaling(KScaling type, double param) {
		this.type = type;
		this.param = param;
	}

}
