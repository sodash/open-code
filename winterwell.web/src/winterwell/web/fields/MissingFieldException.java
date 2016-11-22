package winterwell.web.fields;

import java.util.Collections;
import java.util.List;

import winterwell.web.WebInputException;

import com.winterwell.utils.Printer;

public class MissingFieldException extends WebInputException {
	private static final long serialVersionUID = 1L;

	public MissingFieldException(AField field) {
		super("Missing field " + field);
		assert field != null;
		this.fields = Collections.singletonList(field);
	}

	public MissingFieldException(List<AField> fields) {
		super("Missing fields " + Printer.toString(fields));
		this.fields = fields;
	}

	public List<AField> getFields() {
		return fields;
	}

}
