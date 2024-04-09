/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.reference;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * @param <T> The expected returned type.
 */
@Incubating
public interface TypedFieldReference<T> extends FieldReference {

	Class<T> type();

	default ValueConvert valueConvert() {
		return ValueConvert.YES;
	}

}
