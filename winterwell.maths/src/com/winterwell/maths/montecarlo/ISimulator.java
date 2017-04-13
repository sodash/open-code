package com.winterwell.maths.montecarlo;

import com.winterwell.utils.time.Dt;

public interface ISimulator {

	Particle sim1step(Particle particle, Dt timeStep);

}
