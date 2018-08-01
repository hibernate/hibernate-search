/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;


public abstract class AbstractMatchPredicateBuilder<T> extends AbstractSearchPredicateBuilder
		implements MatchPredicateBuilder<LuceneSearchPredicateBuilder> {

	protected final String absoluteFieldPath;

	private final LuceneFieldConverter<T> converter;

	protected T value;

	protected AbstractMatchPredicateBuilder(String absoluteFieldPath, LuceneFieldConverter<T> converter) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.converter = converter;
	}

	@Override
	public void value(Object value) {
		this.value = converter.convertFromDsl( value );
	}
}
