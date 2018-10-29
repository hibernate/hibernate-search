/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

public class StringFieldSortBuilderFactory extends AbstractStandardLuceneFieldSortBuilderFactory<String> {

	public StringFieldSortBuilderFactory(Sortable sortable, LuceneFieldConverter<String, ?> converter) {
		super( sortable, converter );
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> createFieldSortBuilder(String absoluteFieldPath) {
		checkSortable( absoluteFieldPath );

		return new StringFieldSortBuilder( absoluteFieldPath, converter );
	}
}
