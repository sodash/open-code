package com.winterwell.nlp.corpus.wordnet;

import java.io.File;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Pointer;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.Word;
import net.didion.jwnl.dictionary.Dictionary;
import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.io.FileUtils;

/**
 * Provide access to WordNet via JWNL.
 * <p>
 * Since JWNL is a Java wrapper for wordnet, the purpose of this class is really
 * just to hide JWNL's initialisation warts.
 * <p>
 * The only dictionary methods you should really ever need to call are
 * lookupIndexWord(), lookupAllIndexWords(), and getIndexWordIterator().
 * <p>
 * The other methods you may be interested in Relationship.findRelationships(),
 * and those in PointerUtils.
 * <p>
 * Relationship.findRelationships() allows you to find relationships of a given
 * type between two words (such as ancestry). Another way of thinking of a
 * relationship is as a path from the source synset to the target synset.
 * <p>
 * The methods in PointerUtils allow you to find chains of pointers of a given
 * type. For example, calling PointerUtils.getHypernymTree() on the synset that
 * contains "dog," returns a tree with all its parent synsets ("canine"), and
 * its parents' parents ("carnivore"), etc., all the way to the root synset
 * ("entity").
 * <p>
 * JWNL provides support for accessing the WordNet database through three
 * structures - the standard file distribution, a database, or an in-memory map.
 * Utilities are provided to convert from the file structure to an SQL database
 * or in-memory map, and a configuration file controls which system the library
 * uses.
 * <p>
 * TODO support the in-memory map for heavy use
 * 
 * @author daniel
 * 
 */
public class WordNetAccess implements IThesaurus {

	private static boolean initialised;

	static List<String> getSynonyms(POS pos, String word) {
		try {
			IndexWord iw = Dictionary.getInstance().lookupIndexWord(pos, word);
			if (iw == null)
				return null;
			List<String> synonyms = new ArrayList<String>();
			for (Synset set : iw.getSenses()) {
				Pointer[] pointerArr = set.getPointers(PointerType.SIMILAR_TO);
				for (Pointer pointer : pointerArr) {
					Synset synset = pointer.getTargetSynset();
					Word[] words = synset.getWords();
					for (Word w : words) {
						String s = w.getLemma();
						// strip off some strange annotations, e.g. laughing(a)
						if (s.indexOf('(') != -1) {
							s = s.substring(0, s.indexOf('('));
						}
						synonyms.add(s);
					}
				}
			}
			return synonyms;
		} catch (JWNLException e) {
			throw Utils.runtime(e);
		}
	}

	private static void init() {
		if (initialised)
			return;
		File dictDir = NLPWorkshop.get().getFile("WordNet3_0-dict");
		String configXml = FileUtils.read(new File(
				FileUtils.getWinterwellDir(),
				"code/middleware/jwnl/config/file_properties.xml"));
		configXml = configXml.replace("$WORDNET_DICT_DIR",
				dictDir.getAbsolutePath());
		try {
			JWNL.initialize(new StringBufferInputStream(configXml));
		} catch (JWNLException e) {
			// probably either version or missing file
			throw new WrappedException(e);
		}
	}

	public WordNetAccess() {
		init();
	}

	/**
	 * 
	 * @param pos
	 * @param lemma
	 *            word
	 * @return
	 */
	public IndexWord getIndexWord(POS pos, String lemma) {
		try {
			return Dictionary.getInstance().getIndexWord(pos, lemma);
		} catch (JWNLException e) {
			throw Utils.runtime(e);
		}
	}

	@Override
	public Set<String> getSynonyms(String word) {
		Set<String> synonyms = new HashSet<String>();
		for (Object pos : POS.getAllPOS()) {
			List<String> syns = getSynonyms((POS) pos, word);
			if (syns == null) {
				continue;
			}
			synonyms.addAll(syns);
		}
		// always include the word itself
		synonyms.add(word);
		return synonyms;
	}
}
