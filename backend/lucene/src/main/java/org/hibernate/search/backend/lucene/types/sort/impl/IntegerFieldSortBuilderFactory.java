/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

public class IntegerFieldSortBuilderFactory extends AbstractStandardLuceneFieldSortBuilderFactory<Integer> {

	public IntegerFieldSortBuilderFactory(LuceneFieldConverter<Integer, ?> converter) {
		super( converter );
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> createFieldSortBuilder(String absoluteFieldPath) {
		return new IntegerFieldSortBuilder( absoluteFieldPath, converter );
	}
}
