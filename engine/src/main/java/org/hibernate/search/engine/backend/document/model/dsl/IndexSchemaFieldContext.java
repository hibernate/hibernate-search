/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

import java.time.LocalDate;

import org.hibernate.search.engine.spatial.GeoPoint;


/**
 * @author Yoann Rodiere
 */
public interface IndexSchemaFieldContext {

	<F> StandardIndexSchemaFieldTypedContext<?, F> as(Class<F> inputType);

	StringIndexSchemaFieldTypedContext<?> asString();

	StandardIndexSchemaFieldTypedContext<?, Integer> asInteger();

	StandardIndexSchemaFieldTypedContext<?, Long> asLong();

	StandardIndexSchemaFieldTypedContext<?, LocalDate> asLocalDate();

	StandardIndexSchemaFieldTypedContext<?, GeoPoint> asGeoPoint();

	// TODO NumericBridgeProvider
	// TODO JavaTimeBridgeProvider
	// TODO BasicJDKTypesBridgeProvider

	default <T> T extension(IndexSchemaFieldContextExtension<T> extension) {
		return extension.extendOrFail( this );
	}

}
