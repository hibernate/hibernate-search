/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl.impl;

import java.util.Collection;
import java.util.Locale;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterTagSchema;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterBoundaryScannerLocaleOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterBoundaryScannerTypeFastVectorHighlighterOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFastVectorHighlighterOptionsStep;
import org.hibernate.search.engine.search.highlighter.spi.BoundaryScannerType;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterBuilder;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.util.common.impl.Contracts;

public class HighlighterFastVectorHighlighterOptionsStepImpl
		extends HighlighterOptionsStepImpl<HighlighterFastVectorHighlighterOptionsStep>
		implements HighlighterFastVectorHighlighterOptionsStep {

	public HighlighterFastVectorHighlighterOptionsStepImpl(
			SearchHighlighterBuilder highlightBuilder) {
		super( highlightBuilder );
		this.highlighterBuilder.type( SearchHighlighterType.FAST_VECTOR );
	}

	@Override
	public HighlighterFastVectorHighlighterOptionsStep boundaryMaxScan(int max) {
		highlighterBuilder.boundaryMaxScan( max );
		return this;
	}

	@Override
	public HighlighterFastVectorHighlighterOptionsStep boundaryChars(String boundaryChars) {
		highlighterBuilder.boundaryChars( boundaryChars );
		return this;
	}

	@Override
	public HighlighterFastVectorHighlighterOptionsStep boundaryChars(Character[] boundaryChars) {
		HighlighterFastVectorHighlighterOptionsStepImpl.this.highlighterBuilder.boundaryChars( boundaryChars );
		return this;
	}

	@Override
	public HighlighterFastVectorHighlighterOptionsStep phraseLimit(int limit) {
		this.highlighterBuilder.phraseLimit( limit );
		return this;
	}

	@Override
	public HighlighterFastVectorHighlighterOptionsStep tags(Collection<String> preTags, String postTag) {
		highlighterBuilder.clearTags();
		highlighterBuilder.tags( preTags, postTag );
		return this;
	}

	@Override
	public HighlighterFastVectorHighlighterOptionsStep tags(Collection<String> preTags, Collection<String> postTags) {
		highlighterBuilder.clearTags();
		highlighterBuilder.tags( preTags, postTags );
		return this;
	}

	@Override
	public HighlighterFastVectorHighlighterOptionsStep tagSchema(HighlighterTagSchema tagSchema) {
		highlighterBuilder.clearTags();
		highlighterBuilder.tagSchema( tagSchema );
		return this;
	}

	@Override
	public HighlighterBoundaryScannerTypeFastVectorHighlighterOptionsStep<? extends HighlighterFastVectorHighlighterOptionsStep> boundaryScanner() {
		return new HighlighterBoundaryScannerTypeFastVectorHighlighterOptionsStepImpl();
	}

	private class HighlighterBoundaryScannerTypeFastVectorHighlighterOptionsStepImpl
			implements
			HighlighterBoundaryScannerTypeFastVectorHighlighterOptionsStep<HighlighterFastVectorHighlighterOptionsStep> {

		@Override
		public HighlighterBoundaryScannerLocaleOptionsStep<HighlighterFastVectorHighlighterOptionsStep> chars() {
			HighlighterFastVectorHighlighterOptionsStepImpl.this.highlighterBuilder.boundaryScannerType(
					BoundaryScannerType.CHARS );
			return new HighlighterBoundaryScannerLocaleOptionsStepImpl();
		}

		@Override
		public HighlighterBoundaryScannerLocaleOptionsStep<HighlighterFastVectorHighlighterOptionsStep> sentence() {
			HighlighterFastVectorHighlighterOptionsStepImpl.this.highlighterBuilder.boundaryScannerType(
					BoundaryScannerType.SENTENCE );
			return new HighlighterBoundaryScannerLocaleOptionsStepImpl();
		}

		@Override
		public HighlighterBoundaryScannerLocaleOptionsStep<HighlighterFastVectorHighlighterOptionsStep> word() {
			HighlighterFastVectorHighlighterOptionsStepImpl.this.highlighterBuilder.boundaryScannerType(
					BoundaryScannerType.WORD );
			return new HighlighterBoundaryScannerLocaleOptionsStepImpl();
		}
	}

	private class HighlighterBoundaryScannerLocaleOptionsStepImpl
			implements
			HighlighterBoundaryScannerLocaleOptionsStep<HighlighterFastVectorHighlighterOptionsStep> {

		@Override
		public HighlighterFastVectorHighlighterOptionsStep locale(Locale locale) {
			Contracts.assertNotNull( locale, "locale" );
			HighlighterFastVectorHighlighterOptionsStepImpl.this.highlighterBuilder.boundaryScannerLocale( locale.toString() );
			return HighlighterFastVectorHighlighterOptionsStepImpl.this;
		}
	}
}
