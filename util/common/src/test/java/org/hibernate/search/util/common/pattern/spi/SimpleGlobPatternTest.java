/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.pattern.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SimpleGlobPatternTest {

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		return new Object[][] {
				{
						"",
						Collections.singleton( "" ),
						Arrays.asList( "foo", "bar" ),
						"<EMPTY>",
						"",
						null },
				{
						"*",
						Arrays.asList( "", "foo", "bar" ),
						Collections.emptyList(),
						"*",
						"*",
						null },
				{
						"**",
						Arrays.asList( "", "foo", "bar" ),
						Collections.emptyList(),
						"*",
						"*",
						null },
				{
						"a",
						Collections.singleton( "a" ),
						Arrays.asList( "", "aa", "ab", "b" ),
						"a",
						"a",
						"a" },
				{
						"foo",
						Collections.singleton( "foo" ),
						Arrays.asList( "", "fooo", "afoo", "bar", "foobar", "fo", "oo" ),
						"foo",
						"foo",
						"foo" },
				{
						"*a",
						Arrays.asList( "a", "aa", "ba", "baa" ),
						Arrays.asList( "", "ab", "bab", "b" ),
						"*a",
						"*a",
						null },
				{
						"*foo",
						Arrays.asList( "foo", "afoo", "barfoo" ),
						Arrays.asList( "", "fooo", "bar", "foobar", "fo", "oo" ),
						"*foo",
						"*foo",
						null },
				{
						"a*",
						Arrays.asList( "a", "aa", "ab" ),
						Arrays.asList( "", "bab", "b", "ba", "baa" ),
						"a*",
						"a*",
						null },
				{
						"foo*",
						Arrays.asList( "foo", "foobar", "fooo" ),
						Arrays.asList( "", "afoo", "bar", "barfoo", "fo", "oo" ),
						"foo*",
						"foo*",
						null },
				{
						"a*b",
						Arrays.asList( "ab", "aab", "abb", "afoob" ),
						Arrays.asList( "", "bab", "tab", "bat", "a", "b" ),
						"a*b",
						"a*b",
						null },
				{
						"a**b",
						Arrays.asList( "ab", "aab", "abb", "afoob" ),
						Arrays.asList( "", "bab", "tab", "bat", "a", "b" ),
						"a*b",
						"a*b",
						null },
				{
						"a*b*",
						Arrays.asList( "ab", "aab", "abb", "aba", "abab", "afoob", "afoobfoo" ),
						Arrays.asList( "", "bab", "tab", "bat", "a", "b" ),
						"a*b*",
						"a*b*",
						null },
				// Backtracking
				{
						"foo*bar*boo",
						Arrays.asList( "foobarboo", "fooobarboo", "foobaraboo", "foobaraboo", "fooobaraboo" ),
						Arrays.asList( "", "foo", "foobar", "foobara", "foobarbooa", "foobarbaz" ),
						"foo*bar*boo",
						"foo*bar*boo",
						null }
		};
	}

	private final String patternString;
	private final SimpleGlobPattern pattern;
	private final Collection<String> expectedMatching;
	private final Collection<String> expectedNonMatching;
	private final String expectedToString;
	private final String expectedToPatternString;
	private final Optional<String> expectedToLiteral;

	public SimpleGlobPatternTest(String patternString, Collection<String> expectedMatching,
			Collection<String> expectedNonMatching,
			String expectedToString, String expectedToPatternString, String expectedToLiteral) {
		this.patternString = patternString;
		this.pattern = SimpleGlobPattern.compile( patternString );
		this.expectedMatching = expectedMatching;
		this.expectedNonMatching = expectedNonMatching;
		this.expectedToString = expectedToString;
		this.expectedToPatternString = expectedToPatternString;
		this.expectedToLiteral = Optional.ofNullable( expectedToLiteral );
	}

	@Test
	public void testToString() {
		assertThat( pattern.toString() ).isEqualTo( expectedToString );
	}

	@Test
	public void testEqualsAndHashCode() {
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

	@Test
	public void matches() {
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

	@Test
	public void toPatternString() {
		assertThat( pattern.toPatternString() ).isEqualTo( expectedToPatternString );
	}

	@Test
	public void toLiteral() {
		assertThat( pattern.toLiteral() ).isEqualTo( expectedToLiteral );
	}
}
