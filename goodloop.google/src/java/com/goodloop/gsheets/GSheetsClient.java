package com.goodloop.gsheets;

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
 * @testedby GSheetsClientTest
 * 
 * @author Google, daniel
 *
 */
public class GSheetsClient {
	private static final String APP = "moneyscript.good-loop.com";
	private static final String APPLICATION_NAME = "MoneyScript by Good-Loop";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";
	private static final String LOGTAG = "GSheetsClient";
	/**
	 * The sheet tab within the overall (spread)sheet
	 */
	private int sheet = 0;

	public Spreadsheet getSheet(String id) throws Exception {
		Sheets service = getService();
		Spreadsheet ss = service.spreadsheets().get(id).execute();
		return ss;
	}

	/**
	 * 
	 * @param title optional
	 * @return
	 * @throws Exception
	 */
	public Spreadsheet createSheet(String title) throws Exception {
		Sheets service = getService();
		Spreadsheet ss = new Spreadsheet();	
		if ( ! Utils.isBlank(title)) {
			SpreadsheetProperties sps = new SpreadsheetProperties();
			sps.setTitle(title);
			ss.setProperties(sps);
		}
		Spreadsheet s2 = service.spreadsheets().create(ss).execute();
		String sid = s2.getSpreadsheetId();
		Log.i(LOGTAG, "created spreadsheet "+sid);
		return s2;
	}

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
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

	private static Sheets getService() {
		Log.i(LOGTAG, "getService...");
		try {
			final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName(APPLICATION_NAME).build();
			return service;
		} catch(Exception ex) {
			Log.i(LOGTAG, "getService :( "+ex); // make sure its logged
			throw Utils.runtime(ex);
		}
	}

	/**
	 * See https://developers.google.com/sheets/api/guides/batchupdate#java
	 * 
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public Object updateValues(String spreadsheetId, List<List<Object>> values)
			throws GeneralSecurityException, IOException {
		Log.i(LOGTAG, "updateValues... spreadsheet: "+spreadsheetId);
		Sheets service = getService();
		
		ValueRange body = new ValueRange().setValues(values);

		String valueInputOption = "USER_ENTERED";
		int w = values.get(0).size();
		String c = getBase26(w - 1); // w base 26
		String range = "A1:" + c + values.size();
		// TODO sheet number 
		UpdateValuesResponse result = service.spreadsheets().values()
				.update(spreadsheetId, range, body)
				.setValueInputOption(valueInputOption).execute();
		String ps = result.toPrettyString();
		Integer cnt = result.getUpdatedCells();
//			System.out.println(ps);
		Log.i(LOGTAG, "updated spreadsheet: "+getUrl(spreadsheetId));
		return result.toPrettyString();
	}
	

	public String getUrl(String spreadsheetId) {
		return "https://docs.google.com/spreadsheets/d/"+spreadsheetId;
	}
	
	static Pattern gsu = Pattern.compile("^https://docs.google.com/spreadsheets/d/([^/]+)");
	
	static public String getSpreadsheetId(String url) {
		// convert a normal url to a gviz
		Matcher m = gsu.matcher(url);
		if ( ! m.find()) {
			return null;			
		}
		String docid = m.group(1);
		return docid;
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
	
	public void clearSpreadsheet(String sid) throws IOException{
		Sheets service = getService();
		List<Request> requests = new ArrayList<>();

		requests.add(new Request()
				.setUpdateCells(new UpdateCellsRequest()
						.setRange(new GridRange()
								.setSheetId(sheet))
						.setFields("*")));
		
		BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
		BatchUpdateSpreadsheetResponse response = service.spreadsheets().batchUpdate(sid, body).execute();
	}

	/**
	 * GoogleSheets doesn't handle null values well, 
	 * ??what happens??
	 * change all null values to an empty space
	 * @param values
	 * @return cleaned values
	 */
	public List<List<Object>> replaceNulls(List<List<Object>> values) {
		List<List<Object>> cleanedValues = new ArrayList();
		for(List<Object> row : values) {
			Collections.replaceAll(row, null, " ");
//			Collections.replaceAll(row, "", " "); ??wanted??
			cleanedValues.add(row);
		}
		return cleanedValues;
	}	

}
