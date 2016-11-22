//package com.winterwell.datalog.server;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import no.uib.cipr.matrix.Vector;
//
//import com.winterwell.datalog.Stat;
//
//import winterwell.maths.chart.RenderWithFlot;
//import winterwell.maths.timeseries.Datum;
//import winterwell.maths.timeseries.IDataStream;
//import winterwell.maths.timeseries.ListDataStream;
//import com.winterwell.utils.containers.ArrayMap;
//import com.winterwell.utils.time.TUnit;
//import com.winterwell.utils.time.Time;
//import winterwell.web.ajax.JsonResponse;
//
///**
// TODO copy back from StatServlet
// * code to make writing a servlet easy.
// * 
// * json export
// * chart drawing
// * label selection
// * 
// * @author daniel
// *
// */
//public class ProtoServlet {	
//	RenderWithFlot render = new RenderWithFlot();
//	
//	/**
//	 * 
//	 * @param ids
//	 * @param start
//	 * @param end
//	 * @return List of {data:double[] values, label:String} maps
//	 */
//	 List<Map> getJson(List<String> ids, Time start, Time end) {
//		if (start==null && end==null) {
//			end = new Time();
//		}
//		if (start==null) start = end.minus(TUnit.WEEK);
//		else if (end==null) end = start.plus(TUnit.WEEK);
//		
//		// a map per id (chart-line), with x=timestamp
//		ListDataStream data = ((IDataStream)Stat.getData(ids, start, end)).list();
//		
//		// copy it out into chart-lines
//		List<Map> dlist = new ArrayList(ids.size());
//		for(int i=0; i<data.getDim(); i++) {
//			ArrayList<double[]> vals = new ArrayList<double[]>();
//			ArrayMap data_i = new ArrayMap("data", vals, "label", ids.get(i));			
//			for (Datum v : data) {
//				double x = v.time.getTime();
//				double y = v.get(i);
//				vals.add(new double[] { x, y });
//			}
//			dlist.add(data_i);
//		}
//		return dlist;
//	}
//}
