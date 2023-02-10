/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl.impl;

import java.util.Locale;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterBoundaryScannerLocaleOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterBoundaryScannerTypeOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterUnifiedOptionsStep;
import org.hibernate.search.engine.search.highlighter.spi.BoundaryScannerType;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterBuilder;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.util.common.impl.Contracts;

public class HighlighterUnifiedOptionsStepImpl
		extends HighlighterOptionsStepImpl<HighlighterUnifiedOptionsStep>
		implements HighlighterUnifiedOptionsStep {

	public HighlighterUnifiedOptionsStepImpl(SearchHighlighterBuilder highlightBuilder) {
		super( highlightBuilder );
		this.highlighterBuilder.type( SearchHighlighterType.UNIFIED );
	}

	@Override
	public HighlighterUnifiedOptionsStep maxAnalyzedOffset(int max) {
		highlighterBuilder.maxAnalyzedOffset( max );
		return this;
	}

	@Override
	public HighlighterBoundaryScannerTypeOptionsStep<? extends HighlighterUnifiedOptionsStep> boundaryScanner() {
		return new HighlighterBoundaryScannerTypeOptionsStepImpl();
	}

	private class HighlighterBoundaryScannerTypeOptionsStepImpl
			implements HighlighterBoundaryScannerTypeOptionsStep<HighlighterUnifiedOptionsStep> {

		@Override
		public HighlighterBoundaryScannerLocaleOptionsStep<HighlighterUnifiedOptionsStep> sentence() {
			HighlighterUnifiedOptionsStepImpl.this.highlighterBuilder.boundaryScannerType( BoundaryScannerType.SENTENCE );
			return new HighlighterBoundaryScannerLocaleOptionsStepImpl();
		}

		@Override
		public HighlighterBoundaryScannerLocaleOptionsStep<HighlighterUnifiedOptionsStep> word() {
			HighlighterUnifiedOptionsStepImpl.this.highlighterBuilder.boundaryScannerType( BoundaryScannerType.WORD );
			return new HighlighterBoundaryScannerLocaleOptionsStepImpl();
		}
	}

	private class HighlighterBoundaryScannerLocaleOptionsStepImpl
			implements
			HighlighterBoundaryScannerLocaleOptionsStep<HighlighterUnifiedOptionsStep> {

		@Override
		public HighlighterUnifiedOptionsStep locale(Locale locale) {
			Contracts.assertNotNull( locale, "locale" );
			HighlighterUnifiedOptionsStepImpl.this.highlighterBuilder.boundaryScannerLocale( locale.toString() );
			return HighlighterUnifiedOptionsStepImpl.this;
		}
	}
}
