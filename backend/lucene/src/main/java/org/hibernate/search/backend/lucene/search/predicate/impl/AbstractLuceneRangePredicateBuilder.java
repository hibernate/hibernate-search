/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.util.impl.common.LoggerFactory;


public abstract class AbstractLuceneRangePredicateBuilder<F> extends AbstractLuceneSearchPredicateBuilder
		implements RangePredicateBuilder<LuceneSearchPredicateBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;

	protected final String absoluteFieldPath;

	protected final LuceneFieldConverter<?, F> converter;

	protected F lowerLimit;

	protected boolean excludeLowerLimit = false;

	protected F upperLimit;

	protected boolean excludeUpperLimit = false;

	protected AbstractLuceneRangePredicateBuilder(
			LuceneSearchContext searchContext,
			String absoluteFieldPath, LuceneFieldConverter<?, F> converter) {
		this.searchContext = searchContext;
		this.absoluteFieldPath = absoluteFieldPath;
		this.converter = converter;
	}

	@Override
	public void lowerLimit(Object value) {
		try {
			lowerLimit = converter.convertDslToIndex( value, searchContext.getToDocumentFieldValueConvertContext() );
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter(
					e.getMessage(), e, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
	}

	@Override
	public void excludeLowerLimit() {
		excludeLowerLimit = true;
	}

	@Override
	public void upperLimit(Object value) {
		try {
			upperLimit = converter.convertDslToIndex( value, searchContext.getToDocumentFieldValueConvertContext() );
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter(
					e.getMessage(), e, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
	}

	@Override
	public void excludeUpperLimit() {
		excludeUpperLimit = true;
	}
}
