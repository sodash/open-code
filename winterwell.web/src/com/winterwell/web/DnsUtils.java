package com.winterwell.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;
import org.xbill.DNS.ZoneTransferException;
import org.xbill.DNS.ZoneTransferIn;

import com.winterwell.utils.log.Log;

/**
 * DNS utilities. Basically wrappers around the antique JavaDNS library.
 *
 * The methods in the class all involve network activity and are *sloooooow*
 *
 * @testedby DnsUtilsTest
 * @author Joe Halliwell <joe@winterwell.com>, Daniel
 */
public class DnsUtils {

	/**
	 * Retrieve a list of hosts (A records from the specified domain). This
	 * excludes wildcards and the canonical host e.g. soda.sh. WARNING! This
	 * will fail if you pass in something that isn't a domain!
	 *
	 * @param domain
	 *            the domain to query e.g. soda.sh. Must not include a
	 *            subdomain, e.g. egan.soda.sh would be wrong.
	 * @return a list of hostnames (without the domain part)
	 * @throws IOException
	 *             on network errors
	 * @throws ZoneTransferException
	 *             if the domain doesn't want us to list its hosts (a security
	 *             exception of sorts)
	 */
	public static List<String> getHosts(String domain) throws IOException,
			ZoneTransferException {
		Log.d("dnsutils", "Looking up hosts for " + domain);
		List<Record> records = getZoneRecords(domain);

		// Filter the records for A, not wildcard, not canonical
		Name canonicalHost = new Name(domain + ".");
		List<String> hosts = new ArrayList<String>(records.size());
		for (Record record : records) {
			if (!(record instanceof ARecord))
				continue;
			Name name = record.getName();
			if (name.isWild())
				continue;
			if (name.equals(canonicalHost))
				continue;
			String hostname = record.getName().getLabelString(0);
			hosts.add(hostname);
		}
		return hosts;
	}

	/**
	 * Get the CNAME map for the specified domain. This is a map of aliases to
	 * hostnames.
	 *
	 * @param domain
	 *            E.g. "soda.sh"
	 * @return
	 * @throws ZoneTransferException
	 *             if the domain doesn't want us to list its hosts (a security
	 *             exception of sorts)
	 * @throws IOException
	 */
	public static Map<String, String> getAliases(String domain)
			throws IOException, ZoneTransferException {
		Map<String, String> results = new HashMap<String, String>();
		List<Record> records = getZoneRecords(domain);
		for (Record record : records) {
			if (!(record instanceof CNAMERecord))
				continue;
			Name alias = record.getName();
			Name target = ((CNAMERecord) record).getTarget();
			results.put(alias.getLabelString(0), target.getLabelString(0));
		}
		return results;
	}

	/**
	 * Utility function to look up the nameserver responsible for a domain and
	 * retrieve all records from it via a zone transfer. This requires that the
	 * nameserver permits such transfers, which it will in the case of
	 * Winterwell infrastructure, but not in the general case. TODO: Consider
	 * cacheing this.
	 *
	 * @param domain
	 *            the domain to query
	 * @return all records associated with the domain
	 * @throws IOException
	 * @throws ZoneTransferException
	 *             if the domain doesn't want us to list its hosts (a security
	 *             exception of sorts)
	 */
	public static List<Record> getZoneRecords(String domain)
			throws IOException, ZoneTransferException {
		// First lookup the name server for the domain
		Lookup nsLookup = new Lookup(new Name(domain + "."), Type.NS);
		Record[] ns = nsLookup.run();
		if (ns == null || ns.length == 0)
			throw new RuntimeException(
					"Could not locate name server for domain " + domain);
		Name nameserver = ns[0].getAdditionalName();

		// Now run the zone transfer. Obviously this can fail if the nameserver
		// doesn't like us.
		ZoneTransferIn xfr = ZoneTransferIn.newAXFR(new Name(domain),
				nameserver.toString(), null);
		List<Record> records = xfr.run();
		return records;
	}

	/**
	 *
	 * @param name
	 *            E.g. "scotrail.soda.sh"
	 * @return e.g. "bear"
	 * @throws ZoneTransferException
	 * @throws IOException
	 */
	public static String getServer(String name) throws IOException,
			ZoneTransferException {
		boolean okay = (name.matches("[\\w\\-]+\\.\\w+\\.\\w+") || (name.matches("\\w+\\.\\w+")));
		assert okay : name;
		int i = name.indexOf('.');
		int i2 = name.indexOf('.', i+1);
		String domain = i2==-1? name : name.substring(i + 1);
		Map<String, String> aka = getAliases(domain);
		String s = aka.get(name.substring(0, i));
		return s;
	}

}
