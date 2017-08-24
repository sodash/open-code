package com.winterwell.nlp.dict;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.Utils;

public class CMUDictTest {

	@Test
	public void testRhyme() {
		CMUDict cmud = new CMUDict().load();
		String[] phonemes1 = cmud.getPhonemes("like");
		String[] phonemes2 = cmud.getPhonemes("mistake");
		String[] phonemes3 = cmud.getPhonemes("bike");
		assert phonemes1[phonemes1.length-1].equals(phonemes3[phonemes3.length-1]);
		List<String> rhymes = cmud.getRhymes("like");
		System.out.println(rhymes);
	}
	
	@Test
	public void testStress() {
		CMUDict cmud = new CMUDict().load();		
		int[] phonemes1 = cmud.getStressPattern("I");
		assert Arrays.equals(phonemes1, new int[]{1});		
		int[] phonemes2 = cmud.getStressPattern("computer");	
		assert Arrays.equals(phonemes2, new int[]{0,1,0});
	}
	
	@Test
	public void testGetSyllableCount() {
		CMUDict cmud = new CMUDict().load();
		int sc = cmud.getSyllableCount("Hello");
		assert sc > 0;
		System.out.println(Utils.getRandomSelection(10, cmud.getAllWords()));
	}

}
