package com.winterwell.maths.datastorage;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.web.XStreamUtils;

public class HalfLifeIndexTest {

	@Test
	public void testBasicIndexOps() {
		IIndex<String> index = new HalfLifeIndex<String>(10000);
		assert index.indexOf("A") == -1;
		assert index.size() == 0;

		int ia = index.indexOfWithAdd("A");
		assert ia != -1;
		assert ia == index.indexOf("A");
		assert ia == index.indexOfWithAdd("A");

		int ib = index.add("B");
		assert ib != ia;
		assert ib == index.indexOfWithAdd("B");
		assert index.size() == 2;

		List<String> list = Containers.getList(index);
		assert list.size() == 2;
		assert list.contains("A") && list.contains("B");

		assert index.get(ia).equals("A");
		assert index.get(ib).equals("B");

		assert index.contains("A");
		assert index.contains("B");
		assert !index.contains("C");
	}

	@Test
	public void testSerialisation() {
		IIndex<String> index = new HalfLifeIndex<String>(10);
		int ia = index.indexOfWithAdd("A");
		int ib = index.indexOfWithAdd("B");
		int ic = index.indexOfWithAdd("C");
		index.indexOf("B");

		String xml = XStreamUtils.serialiseToXml(index);

		assert !xml.toLowerCase().contains("concurrent");

		HalfLifeIndex index2 = XStreamUtils.serialiseFromXml(xml);
		assert index2.indexOf("C") == ic;

		// wrapped
		ArrayList wrap = new ArrayList();
		wrap.add(index);
		String wxml = XStreamUtils.serialiseToXml(index);

		assert !wxml.toLowerCase().contains("concurrent");

	}

	@Test
	public void testVocabLearning() {
		String[] sa = "1 2 3 4 5 6 7 8 9 10".split(" ");
		String[] sb = "6 7 8 9 10 11 12 13 14 15".split(" ");
		HalfLifeIndex<String> index = new HalfLifeIndex<String>(10);
		// see LOTS of sa
		for (int i = 0; i < 1000; i++) {
			for (String x : sa) {
				index.indexOfWithAdd(x);
			}
		}
		// see some of sb
		for (int i = 0; i < 5; i++) {
			for (String x : sb) {
				index.indexOfWithAdd(x);
			}
		}
		assert index.size() == 15 : index;
		assert index.contains("2");
		assert index.contains("6");
		assert index.contains("11");
		// prune: keep sa
		index.prune();
		assert index.size() == 10;
		assert index.contains("2");
		assert index.contains("6");
		assert !index.contains("11");

		// see lots of sb
		for (int i = 0; i < 100; i++) {
			for (String x : sb) {
				index.indexOfWithAdd(x);
			}
		}

		// prune: keep sb
		index.prune();
		Printer.out(Containers.getList(index));
		assert index.size() == 10;
		assert !index.contains("2");
		assert index.contains("6");
		assert index.contains("11");

	}
}
