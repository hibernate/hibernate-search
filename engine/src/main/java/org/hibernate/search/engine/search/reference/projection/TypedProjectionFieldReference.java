/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.reference.projection;

import org.hibernate.search.engine.search.common.ValueConvert;

public interface TypedProjectionFieldReference<SR, T> extends ProjectionFieldReference<SR> {

	Class<T> projectionType();

	default ValueConvert valueConvert() {
		return ValueConvert.YES;
	}
}
