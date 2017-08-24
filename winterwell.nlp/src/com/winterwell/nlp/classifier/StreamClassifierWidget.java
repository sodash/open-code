package com.winterwell.nlp.classifier;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import com.winterwell.maths.chart.PieChart;
import com.winterwell.maths.chart.Rainbow;
import com.winterwell.maths.chart.RenderWithFlot;
import com.winterwell.maths.stats.distributions.cond.ExplnOfDist;
import com.winterwell.maths.stats.distributions.cond.Sitn;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.utils.IBuildStrings;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.IWidget;

/**
 * Explain what a {@link StreamClassifier} is upto.
 * 
 * @Dependency RenderWithFlot's js dependencies
 * 
 * @author daniel
 * @testedby {@link StreamClassifierWidgetTest} 
 */
public class StreamClassifierWidget implements IWidget {

//	private static final Color TRANSPARENT = new Color(255,255,255,0);
	StreamClassifier classifier;
	private IDocument target;
	private List<String> tags;
	private boolean chartsOn = true;

	public StreamClassifierWidget(StreamClassifier classifier, IDocument target) {
		this.classifier = classifier;
		this.target = target;
		tags = classifier.getTags();
		Collections.sort(this.tags);
	}

	@Override
	public void appendHtmlTo(IBuildStrings bs) {
		appendHtmlTo(bs.sb());
	}

	@Override
	public void appendHtmlTo(StringBuilder sb) {
		sb.append("<div class='StreamClassifierWidget'>\n");
						
		if (target==null) {
			sb.append("No text</div>");
			return;
		}

		// the author model??
		IFiniteDistribution prior = classifier.getPrior();
		// ...plus a bit of doc info
		String xid = "?";
		if (ReflectionUtils.hasMethod(target.getClass(), "getXId")) {
			xid = ""+ ReflectionUtils.invoke(target, "getXId");
		}
		sb.append("<p>XId: "+xid+" Prior: "+prior.asMap()+" Author: "+target.getAuthor()+"</p>");
				
		// classify it
		ExplnOfDist tokenProbs = new ExplnOfDist();
		IFiniteDistribution<String> pTags = classifier.pClassify(target, tokenProbs);
				
		// what did it do?
		String alltext = target.getContents();
		sb.append("<p>"+alltext+"</p>");		

		// colours
		List<String> pTagsList = Containers.getList(pTags);
		if (pTagsList.isEmpty()) pTagsList = classifier.getTags();
		Rainbow rainbow = pTagsList.isEmpty()? new Rainbow<String>(2) : new Rainbow<String>(pTagsList);
		int e = 0;
		int sitnCnt = 0;
		for (Pair2<Sitn, IFiniteDistribution<String>> pair2 : tokenProbs.tokenProbs) {
			// did we skip a stop word or two??
			ExplnOfDist subexpln = tokenProbs.map().get(pair2.first.toString());
			Tkn tkn = (Tkn) pair2.first.outcome;
			String word = tkn.getText();
			if (e != tkn.start && tkn.start != -1) {
				assert tkn.start > 0 : tkn;
				// Text which was skipped by the tokeniser. 
				// NB: tokens can also be skipped
				String skipped = StrUtils.substring(alltext, e, tkn.start);
				sb.append("<span style='color:rgba(128,128,128,128);'>"+skipped+"</span>");				
			}
			if (tkn.end != -1) e = tkn.end;
			assert e >= 0 : tkn;
			
			pair2.second.normalise();
			
			String ml = pair2.second.getMostLikely();
			double p = pair2.second.prob(ml);
			String title = WebUtils.attributeEncode(ml+": "+StrUtils.toNSigFigs(p, 2));
			if (p <= 1.05 / prior.size()) {
				title = "uniform";
			}
			sb.append("<span onclick=\"addArg('sitn',"+sitnCnt+");\" title='"+title+"' ");
											
			if (p > 0.5) {
				Color col = rainbow.getColorSafe(ml);		
				col = new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(255*p));
//			col = GuiUtils.fade(p, TRANSPARENT, col);
				String rgba = WebUtils.color2html(col);
				sb.append("style='color:"+rgba+";' ");
			}
			
			String jsonData = new SimpleJson().toJson(pair2.second.asMap());
			sb.append("data='"+
						WebUtils.attributeEncode(jsonData)+"'>");
			// the word!
			sb.append(tkn.getText());
			if (subexpln !=null && subexpln.skipped) {
				sb.append("<span title='skipped'>_</span>");
			}
			if (tkn.getPOS()!=null) {
				sb.append("<sub>"+tkn.getPOS()+"</sub>");
			}
			
			sb.append("</span> ");			
			sitnCnt++;
		}
		sb.append("<p>" + pTags + "</p>");
		sb.append("<div style='clear:both;'></div>");
		if (chartsOn) {
			PieChart chart = new PieChart("Overall P(tag)", pTags);
			chart.setShowLegend(false);
			chart.setRainbow(rainbow);
			RenderWithFlot render = new RenderWithFlot(300, 300);
			sb.append("<div>"+render.renderToHtml(chart)+"</div>");
		}		
		
		sb.append("<div style='clear:both;'></div>"); // force the pie-chart inside
		sb.append("</div>"); // close 1st div
	}

	/**
	 * Pie-chart? true by default
	 * @param chartsOn
	 */
	public void setCharts(boolean chartsOn) {
		this.chartsOn = chartsOn;		
	}

	/**
	 * @return A String summarising how this was classified the way it was
	 */
	public String getExplanation() {		
		StringBuilder sb = new StringBuilder();
		// the author model??
		IFiniteDistribution prior = classifier.getPrior();
		sb.append("Prior: "+prior.asMap());		
		// classify it
		ExplnOfDist tokenProbs = new ExplnOfDist();
		IFiniteDistribution<String> pTags = classifier.pClassify(target, tokenProbs);
		sb.append(" Posterior: "+pTags.asMap());
		sb.append(" ");
		// colours
		for (Pair2<Sitn, IFiniteDistribution<String>> pair2 : tokenProbs.tokenProbs) {
			// did we skip a stop word or two??
			Tkn tkn = (Tkn) pair2.first.outcome;
			String word = tkn.getText();
			sb.append(word);
			pair2.second.normalise();			
			String ml = pair2.second.getMostLikely();
			double p = pair2.second.prob(ml);			
			if (p > 1.05 / prior.size()) {
				sb.append("("+ml+": "+StrUtils.toNSigFigs(p, 2)+")");
			}			
			sb.append(" ");
		}
		StrUtils.pop(sb, 1);
		return sb.toString();
	}

}
