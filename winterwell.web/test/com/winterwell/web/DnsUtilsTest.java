package com.winterwell.web;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.xbill.DNS.Record;
import org.xbill.DNS.ZoneTransferException;

import com.winterwell.utils.Printer;
import com.winterwell.web.DnsUtils;

public class DnsUtilsTest {

	@Test
	public void testGetHosts() throws IOException, ZoneTransferException {
		{
			List<String> hosts = DnsUtils.getHosts("soda.sh");
			Printer.out(hosts);
			assert hosts.contains("egan");
			assert hosts.contains("brown");
		}
		// The requests below fail
		// {
		// List<String> hosts = DnsUtils.getHosts("egan.soda.sh");
		// System.out.println(hosts);
		// }
		// {
		// List<String> hosts = DnsUtils.getHosts("egan");
		// System.out.println(hosts);
		// }
	}

	@Test
	public void testGetAliases() throws IOException, ZoneTransferException {
		{
			Map<String, String> aliases = DnsUtils.getAliases("soda.sh");
			assert aliases.get("smoketest").equals("haldeman") : aliases.get("smoketest");
			assert aliases.get("winterwell").equals("egan");
		}
		
		// The requests below fail
		// {
		// Map<String, String> aliases = DnsUtils.getAliases("egan.soda.sh");
		// System.out.println(aliases);
		// }
		// {
		// Map<String, String> aliases = DnsUtils.getAliases("scotrail");
		// System.out.println(aliases);
		// }
		// {
		// Map<String, String> aliases =
		// DnsUtils.getAliases("scotrail.soda.sh");
		// System.out.println(aliases);
		// }
	}
	@Test
	public void testGetAliasesAll() throws IOException, ZoneTransferException {
		{
			Map<String, String> aliases = DnsUtils.getAliases("soda.sh");
			for(String alias : aliases.keySet()){
				Printer.out(alias + ":" + aliases.get(alias));
			}
		}
	}

	
	@Test
	public void testGetServer() throws IOException, ZoneTransferException {
		{
			String egan = DnsUtils.getServer("issues.soda.sh");
			assert egan.equals("egan") : egan;
			
			String wolfe = DnsUtils.getServer("smoketest.soda.sh");
			assert wolfe.equals("haldeman") : wolfe;			
			
		}
	}

	@Test
	public void testGetRecords() throws IOException, ZoneTransferException {
		{
			List<Record> aliases = DnsUtils.getZoneRecords("soda.sh");
			System.out.println(aliases);
		}
		// The requests below fail
		// {
		// List<Record> aliases = DnsUtils.getZoneRecords("egan.soda.sh");
		// System.out.println(aliases);
		// }
		// {
		// List<Record> aliases = DnsUtils.getZoneRecords("scotrail");
		// System.out.println(aliases);
		// }
		// {
		// List<Record> aliases = DnsUtils.getZoneRecords("scotrail.soda.sh");
		// System.out.println(aliases);
		// }
	}

}
