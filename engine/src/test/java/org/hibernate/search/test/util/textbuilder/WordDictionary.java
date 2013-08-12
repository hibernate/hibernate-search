/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.util.textbuilder;

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
