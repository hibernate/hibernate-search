/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate;

import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterStep;

/**
 * Allows to specify whether values passed to the DSL should be converted using the
 * {@link IndexFieldTypeConverterStep#dslConverter(ToDocumentFieldValueConverter) DSL converter}
 * defined in the mapping.
 */
public enum DslConverter {

	/**
	 * Enable the DSL converter.
	 * <p>
	 * This generally means the values passed to the DSL are expected to have the exact same type
	 * as the entity property used to populate the index field.
	 * <p>
	 * To be more specific, it means the converter passed to
	 * {@link IndexFieldTypeConverterStep#dslConverter(ToDocumentFieldValueConverter)}
	 * will be applied to values passed to the DSL in order to get a value that the backend can understand.
	 * <p>
	 * If no DSL converter was defined, this option won't have any effect.
	 * <p>
	 * Please refer to the reference documentation for more information.
	 */
	ENABLED,
	/**
	 * Disable the DSL converter.
	 * <p>
	 * This generally means the values passed to the DSL are expected to have the exact same type as the index field.
	 * <p>
	 * To be more specific, it means the converter passed to
	 * {@link IndexFieldTypeConverterStep#dslConverter(ToDocumentFieldValueConverter)}
	 * will <strong>not</strong> be applied to values.
	 * <p>
	 * Please refer to the reference documentation for more information.
	 */
	DISABLED;

	public boolean isEnabled() {
		return this.equals( ENABLED );
	}
}
