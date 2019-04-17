/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

public final class AnnotationDefaultValues {

	/**
	 * This special value is reserved to mark the default of the indexNullAs option.
	 * The default behavior is to not index the null value.
	 */
	public static final String DO_NOT_INDEX_NULL = "__HibernateSearch_indexNullAs_default";

	private AnnotationDefaultValues() {
	}
}
