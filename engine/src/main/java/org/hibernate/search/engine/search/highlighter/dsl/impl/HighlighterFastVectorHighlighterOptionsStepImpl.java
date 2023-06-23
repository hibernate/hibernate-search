/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl.impl;

import java.util.Collection;
import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterBoundaryScannerFastVectorHighlighterOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterBoundaryScannerTypeFastVectorHighlighterStep;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFastVectorHighlighterOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterTagSchema;
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
	public HighlighterBoundaryScannerTypeFastVectorHighlighterStep<
			? extends HighlighterFastVectorHighlighterOptionsStep> boundaryScanner() {
		return new HighlighterBoundaryScannerTypeFastVectorHighlighterStepImpl();
	}

	@Override
	public HighlighterFastVectorHighlighterOptionsStep boundaryScanner(
			Consumer<? super HighlighterBoundaryScannerTypeFastVectorHighlighterStep<?>> boundaryScannerContributor) {
		boundaryScannerContributor.accept( new HighlighterBoundaryScannerTypeFastVectorHighlighterStepImpl() );
		return this;
	}

	private class HighlighterBoundaryScannerTypeFastVectorHighlighterStepImpl
			implements
			HighlighterBoundaryScannerTypeFastVectorHighlighterStep<HighlighterFastVectorHighlighterOptionsStep> {

		@Override
		public HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<HighlighterFastVectorHighlighterOptionsStep> chars() {
			HighlighterFastVectorHighlighterOptionsStepImpl.this.highlighterBuilder.boundaryScannerType(
					BoundaryScannerType.CHARS );
			return new HighlighterBoundaryScannerFastVectorHighlighterOptionsStepImpl();
		}

		@Override
		public HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<
				HighlighterFastVectorHighlighterOptionsStep> sentence() {
			HighlighterFastVectorHighlighterOptionsStepImpl.this.highlighterBuilder.boundaryScannerType(
					BoundaryScannerType.SENTENCE );
			return new HighlighterBoundaryScannerFastVectorHighlighterOptionsStepImpl();
		}

		@Override
		public HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<HighlighterFastVectorHighlighterOptionsStep> word() {
			HighlighterFastVectorHighlighterOptionsStepImpl.this.highlighterBuilder.boundaryScannerType(
					BoundaryScannerType.WORD );
			return new HighlighterBoundaryScannerFastVectorHighlighterOptionsStepImpl();
		}
	}

	private class HighlighterBoundaryScannerFastVectorHighlighterOptionsStepImpl
			implements
			HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<HighlighterFastVectorHighlighterOptionsStep> {

		@Override
		public HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<
				HighlighterFastVectorHighlighterOptionsStep> boundaryMaxScan(
						int max) {
			HighlighterFastVectorHighlighterOptionsStepImpl.this.highlighterBuilder.boundaryMaxScan( max );
			return this;
		}

		@Override
		public HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<
				HighlighterFastVectorHighlighterOptionsStep> boundaryChars(
						String boundaryChars) {
			HighlighterFastVectorHighlighterOptionsStepImpl.this.highlighterBuilder.boundaryChars( boundaryChars );
			return this;
		}

		@Override
		public HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<
				HighlighterFastVectorHighlighterOptionsStep> boundaryChars(Character[] boundaryChars) {
			HighlighterFastVectorHighlighterOptionsStepImpl.this.highlighterBuilder.boundaryChars( boundaryChars );
			return this;
		}

		@Override
		public HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<HighlighterFastVectorHighlighterOptionsStep> locale(
				Locale locale) {
			Contracts.assertNotNull( locale, "locale" );
			HighlighterFastVectorHighlighterOptionsStepImpl.this.highlighterBuilder.boundaryScannerLocale( locale );
			return this;
		}

		@Override
		public HighlighterFastVectorHighlighterOptionsStep end() {
			return HighlighterFastVectorHighlighterOptionsStepImpl.this;
		}
	}
}
