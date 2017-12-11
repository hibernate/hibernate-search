/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.spatial.GeoPoint;


/**
 * @author Yoann Rodiere
 */
public interface FieldModelContext {

	<T> TypedFieldModelContext<T> as(Class<T> inputType);

	TypedFieldModelContext<String> asString();

	TypedFieldModelContext<Integer> asInteger();

	TypedFieldModelContext<LocalDate> asLocalDate();

	TypedFieldModelContext<GeoPoint> asGeoPoint();

	// TODO NumericBridgeProvider
	// TODO JavaTimeBridgeProvider
	// TODO BasicJDKTypesBridgeProvider

	default <T> T withExtension(FieldModelExtension<T> extension) {
		return extension.extendOrFail( this );
	}

}
