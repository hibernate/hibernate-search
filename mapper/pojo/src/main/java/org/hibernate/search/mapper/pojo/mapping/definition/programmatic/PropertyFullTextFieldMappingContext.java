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
 * A context to configure a full-text index field mapped to a POJO property.
 */
public interface PropertyFullTextFieldMappingContext extends PropertyFieldMappingContext<PropertyFullTextFieldMappingContext> {

	/**
	 * @param analyzerName A reference to the analyzer to use for this field.
	 * @return {@code this}, for method chaining.
	 * @see FullTextField#analyzer()
	 */
	PropertyFullTextFieldMappingContext analyzer(String analyzerName);

	/**
	 * @param norms Whether index time scoring information should be stored or not.
	 * @return {@code this}, for method chaining.
	 * @see FullTextField#norms()
	 * @see Norms
	 */
	PropertyFullTextFieldMappingContext norms(Norms norms);

	/**
	 * @param termVector The term vector storing strategy.
	 * @return {@code this}, for method chaining.
	 * @see FullTextField#termVector()
	 * @see TermVector
	 */
	PropertyFullTextFieldMappingContext termVector(TermVector termVector);

}
