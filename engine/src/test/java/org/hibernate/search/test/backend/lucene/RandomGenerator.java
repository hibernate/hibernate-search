/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.backend.lucene;

import org.hibernate.search.test.util.textbuilder.SentenceInventor;

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
