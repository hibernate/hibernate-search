/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

/**
 * A context to configure a keyword index field mapped to a POJO property.
 */
public interface PropertyKeywordFieldMappingContext
		extends PropertyNotFullTextFieldMappingContext<PropertyKeywordFieldMappingContext> {

	/**
	 * @param normalizerName A reference to the normalizer to use for this field.
	 * @return {@code this}, for method chaining.
	 * @see KeywordField#normalizer()
	 */
	PropertyKeywordFieldMappingContext normalizer(String normalizerName);

	/**
	 * @param norms Whether index time scoring information should be stored or not.
	 * @return {@code this}, for method chaining.
	 * @see KeywordField#norms()
	 * @see Norms
	 */
	PropertyKeywordFieldMappingContext norms(Norms norms);

}
