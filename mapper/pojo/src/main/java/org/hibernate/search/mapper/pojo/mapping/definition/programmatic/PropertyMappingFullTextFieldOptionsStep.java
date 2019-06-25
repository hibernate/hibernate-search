/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

/**
 * The step in a property-to-index-field mapping where optional parameters can be set,
 * when the index field is a full-text field.
 */
public interface PropertyMappingFullTextFieldOptionsStep
		extends PropertyMappingFieldOptionsStep<PropertyMappingFullTextFieldOptionsStep> {

	/**
	 * @param analyzerName A reference to the analyzer to use for this field.
	 * @return {@code this}, for method chaining.
	 * @see FullTextField#analyzer()
	 */
	PropertyMappingFullTextFieldOptionsStep analyzer(String analyzerName);

	/**
	 * @param norms Whether index time scoring information should be stored or not.
	 * @return {@code this}, for method chaining.
	 * @see FullTextField#norms()
	 * @see Norms
	 */
	PropertyMappingFullTextFieldOptionsStep norms(Norms norms);

	/**
	 * @param termVector The term vector storing strategy.
	 * @return {@code this}, for method chaining.
	 * @see FullTextField#termVector()
	 * @see TermVector
	 */
	PropertyMappingFullTextFieldOptionsStep termVector(TermVector termVector);

}
