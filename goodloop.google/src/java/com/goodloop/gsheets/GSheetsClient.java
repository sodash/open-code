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
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.BatchUpdate;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.SheetsOperations;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GridCoordinate;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Response;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.UpdateSpreadsheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.web.app.Logins;

import com.google.api.services.sheets.v4.model.Spreadsheet;

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
		File credsFile = Logins.getFile(APP, "credentials.json");
		if (credsFile == null) {
			throw new FileNotFoundException(Logins.getLoginsDir()+"/"+APP+"/credentials.json");
		}
		InputStream in = new FileInputStream(credsFile);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
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
		UpdateValuesResponse result = service.spreadsheets().values().update(spreadsheetId, range, body)
				.setValueInputOption(valueInputOption).execute();
		String ps = result.toPrettyString();
		Integer cnt = result.getUpdatedCells();
//			System.out.println(ps);
		Log.i(LOGTAG, "updated spreadsheet: "+getUrl(spreadsheetId));
		return result.toPrettyString();
	}
	
	void todoUpdateSheet(String spreadsheetId) throws IOException, GeneralSecurityException {
		Sheets service = getService();
		BatchUpdateSpreadsheetRequest bsr = new BatchUpdateSpreadsheetRequest();
		List<Request> reqs = new ArrayList();
		UpdateSheetPropertiesRequest usr = new UpdateSheetPropertiesRequest();
		usr.setFields("*");
		SheetProperties sps = new SheetProperties();
		sps.setSheetId(0);
		GridProperties gps = new GridProperties();
		// freeze
		gps.setFrozenRowCount(1);
		gps.setFrozenColumnCount(1);
		sps.setGridProperties(gps);
		usr.setProperties(sps);
		reqs.add(new Request().setUpdateSheetProperties(usr));
		
		UpdateCellsRequest ucs = new UpdateCellsRequest();
		ucs.setStart(new GridCoordinate().setRowIndex(1).setColumnIndex(1));
		List<RowData> rows = new ArrayList();
		RowData rd = new RowData();
		List<CellData> cells = new ArrayList();
		CellData cd = new CellData();		
		ExtendedValue ev = new ExtendedValue().setFormulaValue("=2+2");
		cd.setEffectiveValue(ev);
		TextFormat tf = new TextFormat();
		tf.setBold(true);
		tf.setItalic(true);
		tf.setForegroundColor(new Color().setRed(0.75f).setGreen(0f).setBlue(0f));
		CellFormat cf = new CellFormat().setTextFormat(tf);
		cd.setEffectiveFormat(cf);
		cells.add(cd);
		rd.setValues(cells);
		rows.add(rd);
		ucs.setRows(rows);
		reqs.add(new Request().setUpdateCells(ucs));
		
		bsr.setRequests(reqs);
		BatchUpdateSpreadsheetResponse br = service.spreadsheets().batchUpdate(spreadsheetId, bsr).execute();		
		List<Response> reps = br.getReplies();
		System.out.println(reps);
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

}