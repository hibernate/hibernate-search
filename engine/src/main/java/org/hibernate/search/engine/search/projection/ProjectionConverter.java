/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterStep;

/**
 * Allows to specify whether projected values should be converted using the
 * {@link IndexFieldTypeConverterStep#projectionConverter(FromDocumentFieldValueConverter) projection converter}
 * defined in the mapping.
 */
public enum ProjectionConverter {

	/**
	 * Enable the projection converter.
	 * <p>
	 * This generally means the projected values will have the same type
	 * as the entity property used to populate the index field.
	 * <p>
	 * To be more specific, it means the converter passed to
	 * {@link IndexFieldTypeConverterStep#projectionConverter(FromDocumentFieldValueConverter)}
	 * will be applied to projected values before returning them.
	 * <p>
	 * If no projection converter was defined, this option won't have any effect.
	 * <p>
	 * Please refer to the reference documentation for more information.
	 */
	ENABLED,
	/**
	 * Disable the projection converter.
	 * <p>
	 * This generally means the projected values will have the same type as the index field.
	 * <p>
	 * To be more specific, it means the converter passed to
	 * {@link IndexFieldTypeConverterStep#projectionConverter(FromDocumentFieldValueConverter)}
	 * will <strong>not</strong> be applied to projected values before returning them.
	 * <p>
	 * Please refer to the reference documentation for more information.
	 */
	DISABLED;

	public boolean isEnabled() {
		return this.equals( ENABLED );
	}
}
