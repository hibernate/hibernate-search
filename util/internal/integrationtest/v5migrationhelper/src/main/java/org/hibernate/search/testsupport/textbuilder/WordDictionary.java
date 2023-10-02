/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.testsupport.textbuilder;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.Set;

/**
 * Test utility meant to build a consistent dictionary of words.
 * This is not just a random generator: like in natural
 * languages shorter terms have a higher frequency in a text corpus
 * and the dictionary size is limited.
 *
 * @author Sanne Grinovero
 */
public class WordDictionary {

	private static final Random r = new Random( 12L );

	private final String[] positionalWords;
	private final int maxSize;
	private final double gaussFactor;

	public WordDictionary(Set<String> words) {
		this.positionalWords = words.toArray( new String[0] );
		//sort by String length. Languages use shorter terms more often.
		Arrays.sort( positionalWords, new StringLengthComparator() );
		maxSize = positionalWords.length;
		gaussFactor = ( (double) maxSize + 1 ) / 4d;
	}

	private static class StringLengthComparator implements Comparator<String>, Serializable {

		@Override
		public int compare(String o1, String o2) {
			return o1.length() - o2.length();
		}

	}

	public String randomWord() {
		int position = Math.abs( (int) ( r.nextGaussian() * gaussFactor ) );
		if ( position < maxSize ) {
			return positionalWords[position];
		}
		else {
			return randomWord();
		}
	}

}
