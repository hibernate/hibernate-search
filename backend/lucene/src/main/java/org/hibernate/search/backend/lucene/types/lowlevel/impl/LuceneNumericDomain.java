/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

public interface LuceneNumericDomain<E> {

	E getMinValue();

	E getMaxValue();

	E getPreviousValue(E value);

	E getNextValue(E value);

	Query createExactQuery(String absoluteFieldPath, E value);

	Query createRangeQuery(String absoluteFieldPath, E lowerLimit, E upperLimit);

	SortField.Type getSortFieldType();

	IndexableField createIndexField(String absoluteFieldPath, E numericValue);

	IndexableField createDocValuesField(String absoluteFieldPath, E numericValue);

}
