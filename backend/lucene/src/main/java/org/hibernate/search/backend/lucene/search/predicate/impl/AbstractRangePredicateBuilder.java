/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.util.impl.common.LoggerFactory;


public abstract class AbstractRangePredicateBuilder<F> extends AbstractSearchPredicateBuilder
		implements RangePredicateBuilder<LuceneSearchPredicateBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final String absoluteFieldPath;

	protected final LuceneFieldConverter<?, F> converter;

	protected F lowerLimit;

	protected boolean excludeLowerLimit = false;

	protected F upperLimit;

	protected boolean excludeUpperLimit = false;

	protected AbstractRangePredicateBuilder(String absoluteFieldPath, LuceneFieldConverter<?, F> converter) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.converter = converter;
	}

	@Override
	public void lowerLimit(Object value) {
		try {
			lowerLimit = converter.convertFromDsl( value );
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
			upperLimit = converter.convertFromDsl( value );
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
