/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.search.DocumentReference;

public class StubHit<T> {
	private final T content;

	public static StubHit<DocumentReference> of(DocumentReference element) {
		return new StubHit<>( element );
	}

	public static StubHit<List<?>> of(Object... elements) {
		return new StubHit<>( new ArrayList<>( Arrays.asList( elements ) ) );
	}

	public static StubHit<List<?>> of(List<?> elements) {
		return new StubHit<>( new ArrayList<>( elements ) );
	}

	private StubHit(T content) {
		this.content = content;
	}

	public T getContent() {
		return content;
	}
}
