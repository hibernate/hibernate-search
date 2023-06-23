/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.textbuilder;

import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Test utility meant to produce sentences of a randomly generated language,
 * having some properties of natural languages.
 * The goal is to produce sentences which look like a western text,
 * but without needing an actual resource to read from so we can create unlimited
 * garbage. We also get a chance to produce some novel poetry.
 * All sentences from the same SentenceInventor will share
 * a limited dictionary, making the frequencies somehow repeatable, suitable to test
 * with Lucene.
 * Sentences produced depend from the constructor arguments,
 * making the output predictable for testing purposes.
 *
 * @author Sanne Grinovero
 */
public class SentenceInventor {

	private final Random r;
	private final WordDictionary dictionary;
	private final Locale randomlocale;
	//array contains repeated object for probability distribution (more chance for a ",")
	private final char[] sentenceSeparators = new char[] { ',', ',', ',', ';', ':', ':' };
	//same as above, but favour the "full stop" char as a more likely end for periods.
	private final char[] periodSeparators = new char[] { '.', '.', '.', '.', '.', '?', '?', '!' };

	/**
	 * @param randomSeed the seed to use for random generator
	 * @param dictionarySize the number of terms to insert in the dictionary used to build sentences
	 */
	public SentenceInventor(long randomSeed, int dictionarySize) {
		r = new Random( randomSeed );
		randomlocale = randomLocale();
		dictionary = randomDictionary( dictionarySize );
	}

	/**
	 * @return a random Locale among the ones available on the current system
	 */
	private Locale randomLocale() {
		Locale[] availableLocales = Locale.getAvailableLocales();
		int index = r.nextInt( availableLocales.length );
		return availableLocales[index];
	}

	/**
	 * @return a random character from the ASCII table (text chars only)
	 */
	public char randomCharacter() {
		return (char) ( r.nextInt( 26 ) + 65 );
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
			return term.toLowerCase( randomlocale );
		}
		else if ( i < 2 ) {
			//completely uppercase in 2/200 cases
			return term;
		}
		else {
			//first letter uppercase in 9/200 cases
			return term.substring( 0, 1 ) + term.substring( 1 ).toLowerCase( randomlocale );
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
			StringBuilder sb = new StringBuilder( sentence[0] );
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
		//Combine two random values to make extreme long/short less likely,
		//But still make the "one statement" period more likely than other shapes.
		int periodLengthSentences = r.nextInt( 6 ) + r.nextInt( 4 ) - 3;
		periodLengthSentences = ( periodLengthSentences < 1 ) ? 1 : periodLengthSentences;
		String firstsentence = nextSentence();
		StringBuilder sb = new StringBuilder()
				.append( firstsentence.substring( 0, 1 ).toUpperCase( randomlocale ) )
				.append( firstsentence.substring( 1 ) );
		for ( int i = 1; i < periodLengthSentences; i++ ) {
			int separatorCharIndex = r.nextInt( sentenceSeparators.length );
			sb
					.append( sentenceSeparators[separatorCharIndex] )
					.append( ' ' )
					.append( nextSentence() );
		}
		int periodSeparatorCharIndex = r.nextInt( periodSeparators.length );
		sb.append( periodSeparators[periodSeparatorCharIndex] );
		sb.append( "\n" );
		return sb.toString();
	}

	//run it to get an idea of what this class is going to produce
	public static void main(String[] args) {
		SentenceInventor wi = new SentenceInventor( 7L, 10000 );
		for ( int i = 0; i < 3000; i++ ) {
			//CHECKSTYLE:OFF
			System.out.print( wi.nextPeriod() );
			//CHECKSTYLE:ON
		}
	}

}
