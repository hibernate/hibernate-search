/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

/**
 * Defines whether the norms should be stored in the index
 *
 * @author Hardy Ferentschik
 * @deprecated Use Hibernate Search 6's text field annotations ({@link KeywordField}, {@link FullTextField})
 * and enable/disable norms with <code>{@link FullTextField#norms() @FullTextField(norms = Norms.YES)}</code>
 * instead.
 */
@Deprecated
public enum Norms {
	/**
	 * Store norms
	 */
	YES,

	/**
	 * Do not store norms
	 */
	NO
}
