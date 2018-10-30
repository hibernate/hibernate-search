/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.time.LocalDate;

import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

public class LocalDateFieldSortBuilderFactory extends AbstractStandardLuceneFieldSortBuilderFactory<LocalDate> {

	public LocalDateFieldSortBuilderFactory(boolean sortable, LuceneFieldConverter<LocalDate, ?> converter) {
		super( sortable, converter );
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> createFieldSortBuilder(String absoluteFieldPath) {
		checkSortable( absoluteFieldPath );

		return new LocalDateFieldSortBuilder( absoluteFieldPath, converter );
	}
}
