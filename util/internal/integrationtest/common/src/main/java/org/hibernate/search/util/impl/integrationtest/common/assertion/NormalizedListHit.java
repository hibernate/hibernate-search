/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;

public class NormalizedListHit {

	public static List<?>[] of(Consumer<Builder> contributor) {
		Builder builder = new Builder();
		contributor.accept( builder );
		return builder.build();
	}

	private NormalizedListHit() {
	}

	public static class Builder {
		private final List<List<?>> expectedHits = new ArrayList<>();

		private Builder() {
		}

		public Builder list(Object firstProjectionItem, Object... otherProjectionItems) {
			List<?> projectionItems = CollectionHelper.asList( firstProjectionItem, otherProjectionItems );
			expectedHits.add( NormalizationUtils.normalize( projectionItems ) );
			return this;
		}

		@SuppressWarnings("rawtypes")
		private List[] build() {
			return expectedHits.toArray( new List[0] );
		}
	}
}
