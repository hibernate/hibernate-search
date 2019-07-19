/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterStep;

/**
 * Specifies whether values should be converted during search queries.
 */
public enum ValueConvert {

	/**
	 * Enables value conversion.
	 * <p>
	 * For field values passed to the DSL (for example the parameter of a match predicate),
	 * the {@link IndexFieldTypeConverterStep#dslConverter(ToDocumentFieldValueConverter) DSL converter}
	 * defined in the mapping will be used.
	 * This generally means values passed to the DSL will be expected to have the same type
	 * as the entity property used to populate the index field.
	 * <p>
	 * For fields values returned by the backend (for example in projections),
	 * the {@link IndexFieldTypeConverterStep#projectionConverter(FromDocumentFieldValueConverter) projection converter}
	 * defined in the mapping will be used.
	 * This generally means the projected values will have the same type
	 * as the entity property used to populate the index field.
	 * <p>
	 * If no converter was defined in the mapping, this option won't have any effect.
	 * <p>
	 * Please refer to the reference documentation for more information.
	 */
	YES,
	/**
	 * Disables value conversion.
	 * <p>
	 * For field values passed to the DSL (for example the parameter of a match predicate),
	 * no converter will be used.
	 * This generally means values passed to the DSL will be expected to have the same type as the index field.
	 * <p>
	 * For fields values returned by the backend (for example in projections),
	 * no converter will be used.
	 * This generally means the projected values will have the same type as the index field.
	 * <p>
	 * Please refer to the reference documentation for more information.
	 */
	NO

}
