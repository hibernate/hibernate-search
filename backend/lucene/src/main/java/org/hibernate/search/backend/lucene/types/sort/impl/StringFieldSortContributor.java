/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;

public class StringFieldSortContributor extends AbstractStandardLuceneFieldSortContributor {

	public static final StringFieldSortContributor INSTANCE = new StringFieldSortContributor();

	private StringFieldSortContributor() {
		super( SortField.STRING_FIRST, SortField.STRING_LAST );
	}

	@Override
	public void contribute(LuceneSearchSortCollector collector, String absoluteFieldPath, SortOrder order, Object missingValue) {
		SortField sortField = new SortField( absoluteFieldPath, SortField.Type.STRING, order == SortOrder.DESC ? true : false );
		setEffectiveMissingValue( sortField, missingValue, order );

		collector.collectSortField( sortField );
	}

	@Override
	public boolean isDslCompatibleWith(LuceneFieldSortContributor other) {
		return INSTANCE == other;
	}
}
