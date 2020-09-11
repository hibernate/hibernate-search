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
 * Whether or not the value is stored in the document
 *
 * @author Emmanuel Bernard
 * @deprecated Use Hibernate Search 6's field annotations ({@link GenericField}, {@link KeywordField},
 * {@link FullTextField}, ...)
 * and enable/disable storage with <code>{@link GenericField#projectable() @GenericField(projectable = Projectable.YES)}</code>
 * instead.
 */
@Deprecated
public enum Store {
	/**
	 * does not store the value in the index
	 */
	NO,
	/**
	 * stores the value in the index
	 */
	YES,
	/**
	 * stores the value in the index in a compressed form
	 */
	COMPRESS
}
