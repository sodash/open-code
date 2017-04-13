package com.winterwell.nlp.chat;

import java.util.List;
import java.util.Map;

import com.winterwell.nlp.classifier.LSlice;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.utils.Key;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.containers.Slice;
import com.winterwell.utils.containers.Tree;

/**
 * 
 * Stackable Has text speaker-ids anaphora-references / entity-references
 * annotations/tags speaker obligations??
 * 
 * @author daniel
 * 
 */
public class Discourse extends Tree<Slice> {

	public static final class DRef {
		Discourse context;
		String id;
	}

	public static final Key<String> PROP_ANAPHORA = new Key("anaphora");

	public static final Key<String> PROP_POS = Tkn.POS;
	public static final Key<String> PROP_SPEAKER = new Key("person");
	private final Map<String, Thingy> name2thing = new ArrayMap();

	final ListMap<String, LSlice> tags = new ListMap();

	public Discourse(Discourse parent, Slice text) {
		super(parent, text);
		assert text != null : this;
		// check text inclusion
		if (parent != null) {
			assert parent.getValue().contains(text) : parent.getValue()
					+ " vs " + text;
		}
	}

	public Discourse(String text) {
		this(null, new Slice(text));
	}

	/**
	 * @param <X>
	 * @param property
	 * @param value
	 *            A span with the property value as it's label. This labelled
	 *            region can spill beyond the slice covered by this Discourse
	 *            object.
	 */
	public <X> void annotate(Key<X> property, LSlice<X> value) {
		assert getValue().overlap(value) != null : value + " not in "
				+ getText();
		tags.add(property.getName(), value);
	}

	public <X> List<LSlice<X>> getAnnotations(Key<X> property) {
		List annos = tags.get(property.getName());
		return annos;
	}

	@Override
	public List<Discourse> getChildren() {
		return (List<Discourse>) super.getChildren();
	}

	@Override
	public Discourse getParent() {
		return (Discourse) super.getParent();
	}

	public Slice getText() {
		return getValue();
	}

	/**
	 * Recursive de-ref of name.
	 * 
	 * @param name
	 *            What is it?
	 * @return Thingy or null
	 */
	public Thingy getThing(String name) {
		Thingy thing = name2thing.get(name);
		if (thing != null)
			return thing;
		if (getParent() == null)
			return null;
		return getParent().getThing(name);
	}
}
