/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.pattern.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SimpleGlobPatternTest {

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( "", Collections.singleton( "" ),
						Arrays.asList( "foo", "bar" ), "<EMPTY>", "", null
				),
				Arguments.of( "*", Arrays.asList( "", "foo", "bar" ),
						Collections.emptyList(), "*", "*", null
				),
				Arguments.of( "**", Arrays.asList( "", "foo", "bar" ),
						Collections.emptyList(), "*", "*", null
				),
				Arguments.of( "a", Collections.singleton( "a" ),
						Arrays.asList( "", "aa", "ab", "b" ), "a", "a", "a"
				),
				Arguments.of( "foo", Collections.singleton( "foo" ),
						Arrays.asList( "", "fooo", "afoo", "bar", "foobar", "fo", "oo" ), "foo", "foo", "foo"
				),
				Arguments.of( "*a", Arrays.asList( "a", "aa", "ba", "baa" ),
						Arrays.asList( "", "ab", "bab", "b" ), "*a", "*a", null
				),
				Arguments.of( "*foo", Arrays.asList( "foo", "afoo", "barfoo" ),
						Arrays.asList( "", "fooo", "bar", "foobar", "fo", "oo" ), "*foo", "*foo", null
				),
				Arguments.of( "a*", Arrays.asList( "a", "aa", "ab" ),
						Arrays.asList( "", "bab", "b", "ba", "baa" ), "a*", "a*", null
				),
				Arguments.of( "foo*", Arrays.asList( "foo", "foobar", "fooo" ),
						Arrays.asList( "", "afoo", "bar", "barfoo", "fo", "oo" ), "foo*", "foo*", null
				),
				Arguments.of( "a*b", Arrays.asList( "ab", "aab", "abb", "afoob" ),
						Arrays.asList( "", "bab", "tab", "bat", "a", "b" ), "a*b", "a*b", null
				),
				Arguments.of( "a**b", Arrays.asList( "ab", "aab", "abb", "afoob" ),
						Arrays.asList( "", "bab", "tab", "bat", "a", "b" ), "a*b", "a*b", null
				),
				Arguments.of( "a*b*", Arrays.asList( "ab", "aab", "abb", "aba", "abab", "afoob", "afoobfoo" ),
						Arrays.asList( "", "bab", "tab", "bat", "a", "b" ), "a*b*", "a*b*", null
				),
				// Backtracking
				Arguments.of( "foo*bar*boo",
						Arrays.asList( "foobarboo", "fooobarboo", "foobaraboo", "foobaraboo", "fooobaraboo" ),
						Arrays.asList( "", "foo", "foobar", "foobara", "foobarbooa", "foobarbaz" ),
						"foo*bar*boo", "foo*bar*boo", null
				)
		);
	}

	@ParameterizedTest(name = "pattern: {0}")
	@MethodSource("params")
	void testToString(String patternString, Collection<String> expectedMatching,
			Collection<String> expectedNonMatching,
			String expectedToString, String expectedToPatternString, String expectedToLiteral) {
		SimpleGlobPattern pattern = SimpleGlobPattern.compile( patternString );
		assertThat( pattern ).hasToString( expectedToString );
	}

	@ParameterizedTest(name = "pattern: {0}")
	@MethodSource("params")
	void testEqualsAndHashCode(String patternString, Collection<String> expectedMatching,
			Collection<String> expectedNonMatching,
			String expectedToString, String expectedToPatternString, String expectedToLiteral) {
		SimpleGlobPattern pattern = SimpleGlobPattern.compile( patternString );
		assertSoftly( softly -> {
			SimpleGlobPattern equalPattern = SimpleGlobPattern.compile( patternString );
			softly.assertThat( pattern )
					.isEqualTo( equalPattern )
					.isNotEqualTo( SimpleGlobPattern.compile( patternString + "a" ) )
					.isNotEqualTo( SimpleGlobPattern.compile( "a" + patternString ) )
					.isNotEqualTo( SimpleGlobPattern.compile( patternString + "a*" ) )
					.isNotEqualTo( SimpleGlobPattern.compile( "a*" + patternString ) );
			softly.assertThat( pattern.hashCode() == equalPattern.hashCode() )
					.as( "Hashcode of .compile(" + patternString + ") is always the same" )
					.isTrue();
		} );
	}

	@ParameterizedTest(name = "pattern: {0}")
	@MethodSource("params")
	void matches(String patternString, Collection<String> expectedMatching,
			Collection<String> expectedNonMatching,
			String expectedToString, String expectedToPatternString, String expectedToLiteral) {
		SimpleGlobPattern pattern = SimpleGlobPattern.compile( patternString );
		assertSoftly( softly -> {
			for ( String candidate : expectedMatching ) {
				softly.assertThat( pattern.matches( candidate ) )
						.as( "'" + patternString + "' matches '" + candidate + "'" )
						.isTrue();
			}
			for ( String candidate : expectedNonMatching ) {
				softly.assertThat( pattern.matches( candidate ) )
						.as( "'" + patternString + "' matches '" + candidate + "'" )
						.isFalse();
			}
		} );
	}

	@ParameterizedTest(name = "pattern: {0}")
	@MethodSource("params")
	void toPatternString(String patternString, Collection<String> expectedMatching,
			Collection<String> expectedNonMatching,
			String expectedToString, String expectedToPatternString, String expectedToLiteral) {
		SimpleGlobPattern pattern = SimpleGlobPattern.compile( patternString );
		assertThat( pattern.toPatternString() ).isEqualTo( expectedToPatternString );
	}

	@ParameterizedTest(name = "pattern: {0}")
	@MethodSource("params")
	void toLiteral(String patternString, Collection<String> expectedMatching,
			Collection<String> expectedNonMatching,
			String expectedToString, String expectedToPatternString, String expectedToLiteral) {
		SimpleGlobPattern pattern = SimpleGlobPattern.compile( patternString );
		assertThat( pattern.toLiteral() ).isEqualTo( Optional.ofNullable( expectedToLiteral ) );
	}
}
