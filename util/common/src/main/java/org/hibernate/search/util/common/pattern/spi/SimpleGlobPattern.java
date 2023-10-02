/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.pattern.spi;

import java.util.Objects;
import java.util.Optional;

/**
 * A simple glob pattern implementation that only supports the {@code *} wildcard.
 * <p>
 * Crucially, this implementation:
 * <ul>
 *     <li>does not rely on regexps and thus does not require any escaping of the pattern string;</li>
 *     <li>allows easily prepending a literal to a given pattern, which is convenient when working with index schemas.</li>
 * </ul>
 */
public abstract class SimpleGlobPattern {

	private static final char WILDCARD_MANY = '*';

	public static SimpleGlobPattern compile(String patternString) {
		SimpleGlobPattern pattern = EmptyGlobPattern.INSTANCE;
		int endIndexInclusive = patternString.length() - 1;
		while ( endIndexInclusive >= 0 ) {
			int lastWildcardIndex = patternString.lastIndexOf( WILDCARD_MANY, endIndexInclusive );
			if ( lastWildcardIndex < endIndexInclusive ) {
				pattern = pattern.prependLiteral( patternString.substring( lastWildcardIndex + 1, endIndexInclusive + 1 ) );
			}
			if ( 0 <= lastWildcardIndex ) {
				pattern = pattern.prependMany();
			}
			endIndexInclusive = lastWildcardIndex - 1;
		}
		return pattern;
	}

	private SimpleGlobPattern() {
	}

	public boolean matches(String candidate) {
		return matches( candidate, 0 );
	}

	public SimpleGlobPattern prependLiteral(String literal) {
		if ( literal.isEmpty() ) {
			return this;
		}
		return new LiteralGlobPattern( literal, this );
	}

	public SimpleGlobPattern prependMany() {
		return new ManyGlobPattern( this );
	}

	public Optional<String> toLiteral() {
		return Optional.empty();
	}

	public abstract String toPatternString();

	protected abstract int minLength();

	protected abstract boolean matches(String candidate, int startIndex);

	private static final class EmptyGlobPattern extends SimpleGlobPattern {
		static final EmptyGlobPattern INSTANCE = new EmptyGlobPattern();

		@Override
		public String toString() {
			return "<EMPTY>";
		}

		@Override
		public String toPatternString() {
			return "";
		}

		@Override
		protected boolean matches(String candidate, int startIndex) {
			return startIndex == candidate.length();
		}

		@Override
		protected int minLength() {
			return 0;
		}
	}

	private static final class LiteralGlobPattern extends SimpleGlobPattern {
		private final String literal;
		private final SimpleGlobPattern next;

		private LiteralGlobPattern(String literal, SimpleGlobPattern next) {
			this.literal = literal;
			this.next = next;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == this ) {
				return true;
			}
			if ( obj == null || getClass() != obj.getClass() ) {
				return false;
			}
			LiteralGlobPattern other = (LiteralGlobPattern) obj;
			return literal.equals( other.literal )
					&& next.equals( other.next );
		}

		@Override
		public int hashCode() {
			return Objects.hash( literal, next );
		}

		@Override
		public String toString() {
			if ( next == EmptyGlobPattern.INSTANCE ) {
				return literal;
			}
			else {
				return literal + next.toString();
			}
		}

		@Override
		protected boolean matches(String candidate, int startIndex) {
			return literal.regionMatches( 0, candidate, startIndex, literal.length() )
					&& next.matches( candidate, startIndex + literal.length() );
		}

		@Override
		public SimpleGlobPattern prependLiteral(String literal) {
			if ( literal.isEmpty() ) {
				return this;
			}
			// Optimization
			return new LiteralGlobPattern( literal + this.literal, next );
		}

		@Override
		public Optional<String> toLiteral() {
			if ( next != EmptyGlobPattern.INSTANCE ) {
				return Optional.empty();
			}
			return Optional.of( literal );
		}

		@Override
		public String toPatternString() {
			return literal + next.toPatternString();
		}

		@Override
		protected int minLength() {
			return literal.length() + next.minLength();
		}
	}

	private static final class ManyGlobPattern extends SimpleGlobPattern {
		private final SimpleGlobPattern next;
		private final int minTailLength;

		private ManyGlobPattern(SimpleGlobPattern next) {
			this.next = next;
			this.minTailLength = next.minLength();
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == this ) {
				return true;
			}
			if ( obj == null || getClass() != obj.getClass() ) {
				return false;
			}
			ManyGlobPattern other = (ManyGlobPattern) obj;
			return next.equals( other.next );
		}

		@Override
		public int hashCode() {
			return Objects.hash( next );
		}

		@Override
		public String toString() {
			if ( next == EmptyGlobPattern.INSTANCE ) {
				return "*";
			}
			else {
				return "*" + next.toString();
			}
		}

		@Override
		protected boolean matches(String candidate, int startIndex) {
			int maxNextStartIndex = candidate.length() - minTailLength;
			// Greedy matching: consume match as much as possible, and backtrack if necessary.
			for ( int i = maxNextStartIndex; i >= startIndex; i-- ) {
				if ( next.matches( candidate, i ) ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public SimpleGlobPattern prependMany() {
			// Optimization
			return this;
		}

		@Override
		public String toPatternString() {
			return WILDCARD_MANY + next.toPatternString();
		}

		@Override
		protected int minLength() {
			return next.minLength();
		}
	}

}
