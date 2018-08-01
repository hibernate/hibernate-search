/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;


public abstract class AbstractRangePredicateBuilder<T> extends AbstractSearchPredicateBuilder
		implements RangePredicateBuilder<LuceneSearchPredicateBuilder> {

	protected final String absoluteFieldPath;

	protected final LuceneFieldConverter<T> converter;

	protected T lowerLimit;

	protected boolean excludeLowerLimit = false;

	protected T upperLimit;

	protected boolean excludeUpperLimit = false;

	protected AbstractRangePredicateBuilder(String absoluteFieldPath, LuceneFieldConverter<T> converter) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.converter = converter;
	}

	@Override
	public void lowerLimit(Object value) {
		lowerLimit = converter.convertFromDsl( value );
	}

	@Override
	public void excludeLowerLimit() {
		excludeLowerLimit = true;
	}

	@Override
	public void upperLimit(Object value) {
		upperLimit = converter.convertFromDsl( value );
	}

	@Override
	public void excludeUpperLimit() {
		excludeUpperLimit = true;
	}
}
