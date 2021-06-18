package com.goodloop.gcal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.Calendar.CalendarList;
import com.google.api.services.calendar.Calendar.Calendars;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GridCoordinate;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Response;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.web.app.Logins;

/**
 * @testedby GCalClientTest
 * 
 * @author Google, daniel
 *
 */
public class GCalClient {
	private static final String APP = "google.good-loop.com";
	private static final String APPLICATION_NAME = "Google Integration for Good-Loop";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";
	private static final String LOGTAG = "GCalClient";


	public GCalClient() {
		// TODO Auto-generated constructor stub
	}
	
	public List getCalendarList() throws IOException {
		Calendar service = getService();
		CalendarList clist = service.calendarList();
		com.google.api.services.calendar.Calendar.CalendarList.List lr = clist.list();
		com.google.api.services.calendar.model.CalendarList clist2 = lr.execute();
		List<CalendarListEntry> items = clist2.getItems();
		return items;
	}
	
	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
//	    private static final String CREDENTIALS_FILE_PATH = "config/credentials.json";

	/**
	 * Creates an authorized Credential object.
	 * 
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		Log.i(LOGTAG, "get credentials...");
		// Load client secrets.
		File credsFile = Logins.getLoginFile(APP, "credentials.json");
		if (credsFile == null) {
			throw new FileNotFoundException(Logins.getLoginsDir()+"/"+APP+"/credentials.json");
		}
		Log.d(LOGTAG, "...read credentials.json "+credsFile);
		InputStream in = new FileInputStream(credsFile);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));		

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		int receiverPort = 7149;
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(receiverPort).build();
		Log.d(LOGTAG, "...get credentials AuthorizationCodeInstalledApp.authorize() with port "+receiverPort+"...");
		AuthorizationCodeInstalledApp acia = new AuthorizationCodeInstalledApp(flow, receiver);
		Credential cred = acia.authorize("user");
		Log.i(LOGTAG, "...get credentials "+cred);
		return cred;
	}

	private static Calendar getService() {
		Log.i(LOGTAG, "getService...");
		try {
			final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			 Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
		                .setApplicationName(APPLICATION_NAME)
		                .build();
			return service;
		} catch(Exception ex) {
			Log.i(LOGTAG, "getService :( "+ex); // make sure its logged
			throw Utils.runtime(ex);
		}
	}

	
	/**
	 * 
	 * @param w
	 * @return 0 = A
	 */
	public static String getBase26(int w) {
		assert w >= 0 : w;
		int a = 'A';
		if (w < 26) {
//			char c = (char) (a + w);
			return Character.toString(a + w);
		}
		int low = w % 26;
		int high = (w / 26) - 1; // -1 'cos this isnt true base 26 -- there's no proper 0 letter
		return getBase26(high) + getBase26(low);
	}
	
}
