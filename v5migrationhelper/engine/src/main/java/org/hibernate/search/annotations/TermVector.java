/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

/**
 * Defines the term vector storing strategy
 *
 * @author John Griffin
 * @deprecated Use Hibernate Search 6's full-text field annotation ({@link FullTextField})
 * and enable/disable term vectors with <code>{@link FullTextField#termVector() @FullTextField(termVector = TermVector.YES)}</code>
 * instead.
 */
@Deprecated
public enum TermVector {
	/**
	 * Store term vectors.
	 */
	YES,
	/**
	 * Do not store term vectors.
	 */
	NO,
	/**
	 * Store the term vector + Token offset information
	 */
	WITH_OFFSETS,
	/**
	 * Store the term vector + token position information
	 */
	WITH_POSITIONS,
	/**
	 * Store the term vector + Token position and offset information
	 */
	WITH_POSITION_OFFSETS
}
