package com.winterwell.maths.stats.distributions.cond;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.graph.DiGraph;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.utils.BestOne;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Pair2;

/**
 * How to explain stuff? Oh well, let's at least have a wrapper class to localise the hacks
 * @author daniel
 *
 */
public class ExplnOfDist {

	public List<Pair2<Sitn, IFiniteDistribution<String>>> tokenProbs = new ArrayList();
	
	public String value;
	/**
	 * Can be null
	 */
	public DiGraph graph;
	
	private Map<String,ExplnOfDist> map;

	/**
	 * If one part of a sequence -- was this item used in the final calculation, or skipped over?
	 */
	public boolean skipped;
	
	public ExplnOfDist() {
		
	}	

	public void set(String string) {
		value = string;
	}

	public DiGraph graph() {
		if (graph==null) graph = new DiGraph();
		return graph;
	}

	/**
	 * @return Sitn.toString() -> explanation for that situation.
	 */
	public Map<String,ExplnOfDist> map() {
		if (map==null) map = new ArrayMap();
		return map;
	}
	
	@Override
	public String toString() {
		return "Expln["+StrUtils.joinWithSkip(", ", value, map, tokenProbs, graph)+"]";
	}

	/**
	 * e.g. "Sitn[dodgyapp.exe | Cntxt[ftr:app, user:zonefox-hq__p.fyfe, user-group:null]] 
        is unlikely for NORMAL (p=0.29596412556053814). 
        Most likely is UNUSUAL (p = 0.7040358744394619). 
        Expln[marginal possibilities:66 count for dodgyapp.exe: 2.0, []]"
	 * @param tag Usually NORMAL, to get an explanation of why this isn't normal.
	 * @return
	 */
	public String simplifyWhyNot(String tag) {
		try {
			String whyNot = value==null? "Why isn't this "+tag+"? " : value+"\n";
			Sitn lowestSitn = null;
			// we should have some per-feature info
			if ( ! Utils.isEmpty(tokenProbs)) {
				// the lowest token-prob
				BestOne<Pair2<Sitn, IFiniteDistribution<String>>> worst = new BestOne<>();
				for (Pair2<Sitn, IFiniteDistribution<String>> sitn_dist : tokenProbs) {		
					try {
						double p = sitn_dist.second.normProb(tag);
						worst.maybeSet(sitn_dist, - p);
					} catch(Throwable ex) {
						// this can throw an error if nan was put in the distribution
					}
				}
				// What can we say about it?
				Pair2<Sitn, IFiniteDistribution<String>> lowestSitnDist = worst.getBest();
				IFiniteDistribution<String> tokenDist = lowestSitnDist.second;
				whyNot += "Most abnormal fact "+pretty(lowestSitnDist.first)+" is unlikely for "+tag+" (p="+Printer.prettyNumber(tokenDist.normProb(tag))
						+"). Most likely is "+tokenDist.getMostLikely()+" (p="+Printer.prettyNumber(tokenDist.normProb(tokenDist.getMostLikely()))+").";
				lowestSitn = lowestSitnDist.first;
			}
			if (map!=null) {
				ExplnOfDist expln = map.get(tag);
				if (expln!=null) {
					whyNot += "\n"+expln; 
				}
				if (lowestSitn!=null) {
					ExplnOfDist explnSitn = map.get(lowestSitn.toString());
					if (explnSitn!=null) {
						whyNot += "\n"+explnSitn.simplifyWhyNot(tag); 
					}
				}
			}
			return whyNot;
		} catch(Throwable ex) {
			return ex.toString();
		}
	}

	private String pretty(Sitn siten) {
		return "seeing "+siten.outcome+" when "+siten.getContext();
	}

}
