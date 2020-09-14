/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

/**
 * Defines how an {@link org.apache.lucene.document.Field} should be indexed.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @deprecated Use Hibernate Search 6's field annotations ({@link GenericField}, {@link KeywordField},
 * {@link FullTextField}, ...)
 * and enable/disable indexing with <code>{@link GenericField#searchable() @GenericField(searchable = Searchable.YES)}</code>
 * instead.
 */
@Deprecated
public enum Index {
	/**
	 * Index the field value.
	 */
	YES,

	/**
	 * Do not index the field value. This field can thus not be searched,
	 * but one can still access its contents provided it is
	 * {@link Store stored}.
	 */
	NO
}
