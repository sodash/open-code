package com.winterwell.depot;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.Printer;

import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.time.Time;

/**
 * Test storing & accessing data with period ranges
 * @author daniel
 *
 */
public class PeriodDataTest {

	Depot depot = Depot.getDefault();
	
	@Test
	public void testSplitIn3() {
		Desc desc = new Desc("periodTest", File.class);
		desc.setTag("test");
		
		// store it
		for (int i=0; i<3; i++) {
			Desc di = new Desc(desc);
			di.setRange(new Time(2012, 1, 1+3*i), new Time(2012, 1, 4+3*i));
			File file = depot.getMetaData(di).getFile();
			file.getParentFile().mkdirs();
			CSVWriter w = new CSVWriter(file, ',');
			for(int j=0; j<3; j++) {
				w.write(new Time(2012, 1, 1+ 3*i +j).getTime(), 1+ i*3 +j);
			}
			w.close();
			depot.put(di, file);
		}
		
		// fetch it all
		Desc<File> dAll = new Desc<File>(desc);
		dAll.setRange(new Time(2012, 1, 1), new Time(2012, 1, 10));		
		
		FileStore fs = ((RemoteStore) ((SlowStorage)depot.base).getBase() ).localStore;
		File dir = fs.getLocalPath(dAll).getParentFile();
		List<File> bits = fs.getRangedData2_bits(dir, dAll);
		assert ! bits.isEmpty();
		
		File data = depot.get(dAll);
		assert data != null;
		System.out.println(data);
		CSVReader csv = new CSVReader(data, ',');
		for (String[] strings : csv) {
			Printer.out(strings);
		}

		// fetch bits
		Desc<File> dBit = new Desc<File>(desc);
		dBit.setRange(new Time(2012, 1, 2), new Time(2012, 1, 8));		
		File dataBit = depot.get(dBit);
		assert dataBit != null;
		System.out.println(dataBit);
		CSVReader csv2 = new CSVReader(dataBit, ',');
		for (String[] strings : csv2) {
			Printer.out(strings);
			Integer i = Integer.valueOf(strings[1]);
			assert i > 1 && i < 9 : strings;			
		}
	}
}
