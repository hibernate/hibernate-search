/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort.impl;

import java.io.IOException;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.SortField;
import org.hibernate.search.exception.SearchException;


/**
 * A SortField extension that simply serves as a data holder, to be detected
 * and interpreted by non-Lucene backend implementations (Elasticsearch in particular).
 * @author Yoann Rodiere
 */
public class NativeSortField extends SortField {

	private static final FieldComparatorSource FAILING_COMPARATOR_SOURCE = new FieldComparatorSource() {
		@Override
		public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException {
			throw new SearchException(
					"This sort field should never have been executed in the first place."
					+ " This is probably a result of queryBuilder.sort().byNative(String) being called while using the Lucene backend."
					+ " This method is only provided for use with the Elasticsearch backend."
			);
		}
	};

	private final String nativeSortDescription;

	public NativeSortField(String fieldName, String nativeSortFieldDescription) {
		/*
		 * Superclass is irrelevant here: we just want to extend SortField to be able
		 * to put this native sort field in a Sort, so that non-standard backends such as Elasticsearch
		 * may integrate the native sort field description (a string) directly in their native sort
		 * representation.
		 */

		super( fieldName, FAILING_COMPARATOR_SOURCE );
		this.nativeSortDescription = nativeSortFieldDescription;
	}

	/**
	 * @return the native sort description
	 */
	public String getNativeSortDescription() {
		return nativeSortDescription;
	}
}
