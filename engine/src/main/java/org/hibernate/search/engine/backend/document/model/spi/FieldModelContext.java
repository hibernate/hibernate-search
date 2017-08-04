/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author Yoann Rodiere
 */
public interface FieldModelContext {

	<T> TypedFieldModelContext<T> from(Class<T> inputType);

	TypedFieldModelContext<String> fromString();

	TypedFieldModelContext<Integer> fromInteger();

	TypedFieldModelContext<LocalDate> fromLocalDate();

	// TODO NumericBridgeProvider
	// TODO JavaTimeBridgeProvider
	// TODO GeoPoint, somehow?
	// TODO BasicJDKTypesBridgeProvider

	<T extends FieldModelContext> Optional<T> unwrap(Class<T> clazz);

}
