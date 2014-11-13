/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.lucene;

import org.hibernate.search.testsupport.textbuilder.SentenceInventor;

import java.util.Random;

/**
 * Generates random phrases, words or numbers
 *
 * @author gustavonalle
 */
@SuppressWarnings("UnusedDeclaration")
public class RandomGenerator {

	private static final int DEFAULT_MAX_WORD_SIZE = 20;
	private final int maxWordSize;
	private static final Random r = new Random();
	private static final SentenceInventor sentenceInventor = new SentenceInventor( 3L, 10000 );

	private RandomGenerator() {
		maxWordSize = DEFAULT_MAX_WORD_SIZE;
	}

	private RandomGenerator(int maxWordSize) {
		this.maxWordSize = maxWordSize;
	}

	public static RandomGenerator withDefaults() {
		return new RandomGenerator( DEFAULT_MAX_WORD_SIZE );
	}

	public static RandomGenerator create(int maxWordSize, int maxPhraseSize) {
		return new RandomGenerator( maxWordSize );
	}

	public int randomIntNotZero(int max) {
		return r.nextInt( max - 1 ) + 1;
	}

	public double randomDouble() {
		return Math.random();
	}

	@SafeVarargs
	public final <T> T oneOf(T... choices) {
		return choices[randomIntNotZero( choices.length + 1 ) - 1];
	}

	public String generateRandomWord() {
		return sentenceInventor.randomString( maxWordSize );
	}

	public String generateRandomPhrase() {
		return sentenceInventor.nextSentence();
	}

}
