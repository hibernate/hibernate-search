/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldSortContributor;
import org.hibernate.search.backend.lucene.document.model.impl.SortMissingValue;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;

abstract class AbstractScalarLuceneFieldSortContributor implements LuceneFieldSortContributor {

	private Object sortMissingValueFirstPlaceholder;

	private Object sortMissingValueLastPlaceholder;

	protected AbstractScalarLuceneFieldSortContributor(Object sortMissingValueFirstPlaceholder, Object sortMissingValueLastPlaceholder) {
		this.sortMissingValueFirstPlaceholder = sortMissingValueFirstPlaceholder;
		this.sortMissingValueLastPlaceholder = sortMissingValueLastPlaceholder;
	}

	protected void setEffectiveMissingValue(SortField sortField, Object missingValue, SortOrder order) {
		if ( missingValue == null ) {
			return;
		}

		// TODO so this is to mimic the Elasticsearch behavior, I'm not totally convinced it's the good choice though
		Object effectiveMissingValue;
		if ( missingValue == SortMissingValue.MISSING_FIRST ) {
			effectiveMissingValue = order == SortOrder.DESC ? sortMissingValueLastPlaceholder : sortMissingValueFirstPlaceholder;
		}
		else if ( missingValue == SortMissingValue.MISSING_LAST ) {
			effectiveMissingValue = order == SortOrder.DESC ? sortMissingValueFirstPlaceholder : sortMissingValueLastPlaceholder;
		}
		else {
			effectiveMissingValue = missingValue;
		}

		sortField.setMissingValue( effectiveMissingValue );
	}
}
