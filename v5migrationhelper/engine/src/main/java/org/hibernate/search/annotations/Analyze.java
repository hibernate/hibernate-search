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
 * Defines whether the field content should be analyzed.
 *
 * @author Hardy Ferentschik
 *
 * @deprecated No longer necessary in Hibernate Search 6.
 * Replace {@link Field} with {@link FullTextField} to define a field with an analyzer,
 * or any other field annotation (e.g. {@link KeywordField}, {@link GenericField}, ...)
 * to define a field without an analyzer.
 */
@Deprecated
public enum Analyze {
	/**
	 * Analyze the field content
	 */
	YES,

	/**
	 * Index field content as is (not analyzed)
	 */
	NO
}
