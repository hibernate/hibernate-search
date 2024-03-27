/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import java.util.Collection;

import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

/**
 * The step in a property-to-index-field mapping where optional parameters can be set,
 * when the index field is a full-text field.
 *
 * @see FullTextField
 */
public interface PropertyMappingFullTextFieldOptionsStep
		extends PropertyMappingStandardFieldOptionsStep<PropertyMappingFullTextFieldOptionsStep> {

	/**
	 * @param analyzerName A reference to the analyzer to use for this field.
	 * @return {@code this}, for method chaining.
	 * @see FullTextField#analyzer()
	 */
	PropertyMappingFullTextFieldOptionsStep analyzer(String analyzerName);

	/**
	 * @param searchAnalyzerName A reference to the analyzer to use for query parameters at search time.
	 * @return {@code this}, for method chaining.
	 * @see FullTextField#searchAnalyzer()
	 */
	PropertyMappingFullTextFieldOptionsStep searchAnalyzer(String searchAnalyzerName);

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

	/**
	 * @param highlightable Whether this field can be highlighted, and if so which highlighter types can be applied to it.
	 * Pass {@code Collections.singleton(Highlightable.NO)} to disable highlighting.
	 * @return {@code this}, for method chaining.
	 * @see FullTextField#highlightable()
	 * @see Highlightable
	 */
	PropertyMappingFullTextFieldOptionsStep highlightable(Collection<Highlightable> highlightable);
}
