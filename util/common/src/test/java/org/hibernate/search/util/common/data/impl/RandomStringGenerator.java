/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.data.impl;

import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import org.hibernate.search.util.common.data.Range;

import org.apache.commons.math3.random.RandomDataGenerator;

class RandomStringGenerator implements StringGenerator {

	private static final char[] CODE_CHARACTERS;
	private static final char[] SENTENCE_CHARACTERS;

	static {
		CODE_CHARACTERS = new char[1 + 1 + 1 + ( '9' - '0' + 1 ) + ( 'Z' - 'A' + 1 ) + ( 'z' - 'a' + 1 )];
		CODE_CHARACTERS[0] = '_';
		CODE_CHARACTERS[1] = '.';
		CODE_CHARACTERS[2] = '-';
		int i = 3;
		for ( char c = '0'; c <= '9'; c++ ) {
			CODE_CHARACTERS[i++] = c;
		}
		for ( char c = 'A'; c <= 'Z'; c++ ) {
			CODE_CHARACTERS[i++] = c;
		}
		for ( char c = 'a'; c <= 'z'; c++ ) {
			CODE_CHARACTERS[i++] = c;
		}

		SENTENCE_CHARACTERS = new char[1 + ( 'z' - 'a' + 1 )];
		SENTENCE_CHARACTERS[0] = ' ';
		i = 1;
		for ( char c = 'a'; c <= 'z'; c++ ) {
			SENTENCE_CHARACTERS[i++] = c;
		}
	}

	static StringGenerator randomCodeStrings(int size) {
		return new RandomStringGenerator(
				"Code characters ([0-9a-zA-Z_.-])", RandomStringGenerator::randomCodeCodepoint, size, size );
	}

	static StringGenerator randomSentenceStrings(int minSize, int maxSize) {
		return new RandomStringGenerator(
				"Sentence characters ([ a-z])", RandomStringGenerator::randomSentenceCodepoint, minSize, maxSize );
	}

	static StringGenerator randomAsciiStrings(int minSize, int maxSize) {
		return new RandomStringGenerator(
				"ASCII", RandomStringGenerator::randomAsciiCodepoint, minSize, maxSize );
	}

	static StringGenerator randomUtf16Strings(int minSize, int maxSize) {
		return new RandomStringGenerator(
				"UTF-16", RandomStringGenerator::randomUtf16Codepoint, minSize, maxSize );
	}

	private final String charsetName;
	private final ToIntFunction<RandomDataGenerator> codepointFunction;
	private final int minSize;
	private final int maxSize;
	private final RandomDataGenerator generator = new RandomDataGenerator();
	private final StringBuilder outBuffer = new StringBuilder();

	private RandomStringGenerator(String charsetName,
			ToIntFunction<RandomDataGenerator> codepointFunction, int minSize, int maxSize) {
		this.charsetName = charsetName;
		this.codepointFunction = codepointFunction;
		this.minSize = minSize;
		this.maxSize = maxSize;
	}

	@Override
	public String toString() {
		return "Random " + charsetName + " strings with size in range " + Range.between( minSize, maxSize );
	}

	@Override
	public Stream<String> stream() {
		return Stream.generate( this::randomString );
	}

	private String randomString() {
		outBuffer.setLength( 0 );
		int size = minSize == maxSize ? minSize : generator.nextInt( minSize, maxSize );
		for ( int i = 0; i < size; i++ ) {
			outBuffer.appendCodePoint( codepointFunction.applyAsInt( generator ) );
		}
		return outBuffer.toString();
	}

	private static int randomCodeCodepoint(RandomDataGenerator generator) {
		return CODE_CHARACTERS[generator.nextInt( 0, CODE_CHARACTERS.length - 1 )];
	}

	private static int randomSentenceCodepoint(RandomDataGenerator generator) {
		return SENTENCE_CHARACTERS[generator.nextInt( 0, SENTENCE_CHARACTERS.length - 1 )];
	}

	private static int randomAsciiCodepoint(RandomDataGenerator generator) {
		return generator.nextInt( 0, 127 );
	}

	private static int randomUtf16Codepoint(RandomDataGenerator generator) {
		int codePoint;
		int type;
		do {
			codePoint = generator.nextInt( Character.MIN_CODE_POINT, Character.MAX_CODE_POINT );
			type = Character.getType( codePoint );
		}
		while ( type == Character.PRIVATE_USE
				|| type == Character.SURROGATE
				|| type == Character.UNASSIGNED );
		return codePoint;
	}
}
