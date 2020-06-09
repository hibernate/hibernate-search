/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types.values;

import java.util.Collections;
import java.util.List;

public abstract class IndexableValues<F> {

	private final List<F> values = Collections.unmodifiableList( create() );

	public List<F> get() {
		return values;
	}

	protected abstract List<F> create();
}
