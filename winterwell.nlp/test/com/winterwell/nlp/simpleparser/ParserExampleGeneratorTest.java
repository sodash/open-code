package com.winterwell.nlp.simpleparser;

import static com.winterwell.nlp.simpleparser.Parsers.lit;
import static com.winterwell.nlp.simpleparser.Parsers.opt;
import static com.winterwell.nlp.simpleparser.Parsers.seq;
import static com.winterwell.nlp.simpleparser.Parsers.word;

import java.util.List;

import org.junit.Test;

import com.winterwell.utils.Printer;

public class ParserExampleGeneratorTest {

	@Test
	public void testSample() {
		Parser setting = seq(lit("Setting:"), 
				Parsers.word("The city", "A country mansion", "The docks"));
		Parser crime = seq(lit("Crime:"), 
				Parsers.word("Murder!", "Theft", "Deception"));
		Parser suspect = Parsers.word("wife", "boyfriend", "friend", "business partner", "business rival", "local criminal");
		Parser criminal = seq(lit("Criminal:"), suspect);
		Parser suspects = seq(lit("Other Suspects:"), suspect, opt(suspect));
		Parser victim = seq(lit("Victim:"), suspect);
		Parser motive = seq(lit("Motive:"),
				Parsers.word("greed", "passion", "revenge", "anger"));
		
		Parser detective = seq(lit("Detective:"), word
				("Parnacki", "Josh", "A passing goose"));
				
		Parser clue = seq(lit("Clue:"), word
				("contradictory statement", "sequence", "knows too much", "by elimination"));
		
		Parser p = Parsers.seq(setting, crime, victim, criminal, suspects, motive, detective, clue);
		
		ParserExampleGenerator egs = new ParserExampleGenerator(p);
		List s = (List) egs.sample();
		String s2 = Printer.toString(s, " \n");
		System.out.println(s2);
	}

}
