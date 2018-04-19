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

public class IntegerFieldSortContributor extends AbstractScalarLuceneFieldSortContributor {

	public static final IntegerFieldSortContributor INSTANCE = new IntegerFieldSortContributor();

	private IntegerFieldSortContributor() {
		super( Integer.MIN_VALUE, Integer.MAX_VALUE );
	}

	@Override
	public void contribute(LuceneSearchSortCollector collector, String absoluteFieldPath, SortOrder order, Object missingValue) {
		SortField sortField = new SortField( absoluteFieldPath, SortField.Type.INT, order == SortOrder.DESC ? true : false );
		setEffectiveMissingValue( sortField, missingValue, order );

		collector.collectSortField( sortField );
	}
}
