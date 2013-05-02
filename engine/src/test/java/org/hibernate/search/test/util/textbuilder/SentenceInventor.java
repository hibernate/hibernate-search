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

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Test utility meant to produce sentences of a randomly generated language,
 * having some properties of natural languages.
 * The goal is to produce sentences which look like a western text,
 * but are not.
 * All sentences from the same SentenceInventor will share
 * a limited dictionary, making the frequencies suitable to test
 * with Lucene.
 * Sentences produced depend from the constructor arguments,
 * making the output predictable for testing purposes.
 *
 * @author Sanne Grinovero
 */
public class SentenceInventor {

	private final Random r;
	private final WordDictionary dictionary;
	//array contains repeated object for probability distribution (more chance for a ",")
	private final char[] sentenceSeparators = new char[] { ',', ',', ',' , ';', ':', ':' };

	/**
	 * @param randomSeed the seed to use for random generator
	 * @param dictionarySize the number of terms to insert in the dictionary used to build sentences
	 */
	public SentenceInventor(long randomSeed, int dictionarySize) {
		r = new Random( randomSeed );
		dictionary = randomDictionary( dictionarySize );
	}

	/**
	 * @return a random character from the ASCII table (text chars only)
	 */
	public char randomCharacter() {
		return (char) (r.nextInt( 26 ) + 65);
	}

	/**
	 * @param length the desired length
	 * @return a randomly generated String
	 */
	public String randomString(int length) {
		char[] chars = new char[length];
		for ( int i = 0; i < length; i++ ) {
			chars[i] = randomCharacter();
		}
		return new String( chars );
	}

	/**
	 * Produces a randomly generated String, using
	 * only western alphabet characters and selecting
	 * the length as a normal distribution of natural languages.
	 * @return the generated String
	 */
	public String randomString() {
		double d = r.nextGaussian() * 6.3d;
		int l = (int) d + 6;
		if ( l > 0 ) {
			return randomString( l );
		}
		else {
			return randomString();
		}
	}

	/**
	 * Produces a random String, which might be lowercase,
	 * completely uppercase, or uppercasing the first char
	 * (randomly selected)
	 * @return produced String
	 */
	public String randomTerm() {
		int i = r.nextInt( 200 );
		String term = randomString();
		if ( i > 10 ) {
			//completely lowercase 189/200 cases
			return term.toLowerCase();
		}
		else if ( i < 2 ) {
			//completely uppercase in 2/200 cases
			return term;
		}
		else {
			//first letter uppercase in 9/200 cases
			return term.substring( 0, 1 ) + term.substring( 1 ).toLowerCase();
		}
	}

	private WordDictionary randomDictionary(int size) {
		Set<String> tree = new TreeSet<String>();
		while ( tree.size() != size ) {
			tree.add( randomTerm() );
		}
		return new WordDictionary( tree );
	}

	/**
	 * Builds a sentence concatenating terms from the generated dictionary and spaces
	 * @return a sentence
	 */
	public String nextSentence() {
		int sentenceLength = r.nextInt( 3 ) + r.nextInt( 10 ) + 1;
		String[] sentence = new String[sentenceLength];
		for ( int i = 0; i < sentenceLength; i++ ) {
			sentence[i] = dictionary.randomWord();
		}
		if ( sentenceLength == 1 ) {
			return sentence[0];
		}
		else {
			StringBuilder sb = new StringBuilder( sentence[0]);
			for ( int i = 1; i < sentenceLength; i++ ) {
				sb.append( " " );
				sb.append( sentence[i] );
			}
			return sb.toString();
		}
	}

	/**
	 * Combines a random (gaussian) number of sentences in a period,
	 * using some punctuation symbols and
	 * capitalizing first char, terminating with dot and newline.
	 * @return
	 */
	public String nextPeriod() {
		int periodLengthSentences = r.nextInt( 7 ) - 2;
		periodLengthSentences = ( periodLengthSentences < 1 ) ? 1 : periodLengthSentences;
		String firstsentence = nextSentence();
		StringBuilder sb = new StringBuilder()
			.append( firstsentence.substring( 0,1 ).toUpperCase() )
			.append( firstsentence.substring( 1 ) );
		for ( int i = 1; i < periodLengthSentences; i++ ) {
			int separatorCharIndex = r.nextInt( sentenceSeparators.length );
			sb
				.append( sentenceSeparators[separatorCharIndex] )
				.append( ' ' )
				.append( nextSentence() );
		}
		sb.append( ".\n" );
		return sb.toString();
	}

	//run it to get an idea of what this class is going to produce
	public static void main(String[] args) {
		SentenceInventor wi = new SentenceInventor( 7L, 10000 );
		for ( int i = 0; i < 30; i++ ) {
			System.out.print( wi.nextPeriod() );
		}
	}

}
