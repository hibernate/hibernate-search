/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl.impl;

import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterBoundaryScannerOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterBoundaryScannerTypeStep;
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
	public HighlighterBoundaryScannerTypeStep<?, ? extends HighlighterUnifiedOptionsStep> boundaryScanner() {
		return new HighlighterBoundaryScannerTypeStepImpl();
	}

	@Override
	public HighlighterUnifiedOptionsStep boundaryScanner(
			Consumer<? super HighlighterBoundaryScannerTypeStep<?, ?>> boundaryScannerContributor) {
		boundaryScannerContributor.accept( new HighlighterBoundaryScannerTypeStepImpl() );
		return this;
	}

	private class HighlighterBoundaryScannerTypeStepImpl
			implements
			HighlighterBoundaryScannerTypeStep<HighlighterBoundaryScannerOptionsStepImpl, HighlighterUnifiedOptionsStep> {

		@Override
		public HighlighterBoundaryScannerOptionsStepImpl sentence() {
			HighlighterUnifiedOptionsStepImpl.this.highlighterBuilder.boundaryScannerType(
					BoundaryScannerType.SENTENCE );
			return new HighlighterBoundaryScannerOptionsStepImpl();
		}

		@Override
		public HighlighterBoundaryScannerOptionsStepImpl word() {
			HighlighterUnifiedOptionsStepImpl.this.highlighterBuilder.boundaryScannerType( BoundaryScannerType.WORD );
			return new HighlighterBoundaryScannerOptionsStepImpl();
		}
	}

	private class HighlighterBoundaryScannerOptionsStepImpl implements
			HighlighterBoundaryScannerOptionsStep<HighlighterBoundaryScannerOptionsStepImpl, HighlighterUnifiedOptionsStep> {

		@Override
		public HighlighterBoundaryScannerOptionsStepImpl locale(Locale locale) {
			Contracts.assertNotNull( locale, "locale" );
			HighlighterUnifiedOptionsStepImpl.this.highlighterBuilder.boundaryScannerLocale( locale );
			return this;
		}

		@Override
		public HighlighterUnifiedOptionsStep end() {
			return HighlighterUnifiedOptionsStepImpl.this;
		}
	}
}
