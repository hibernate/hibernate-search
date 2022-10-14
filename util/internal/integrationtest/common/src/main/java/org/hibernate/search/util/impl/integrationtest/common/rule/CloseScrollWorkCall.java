/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.Set;

public class CloseScrollWorkCall extends Call<CloseScrollWorkCall> {

	private final Set<String> indexNames;

	public CloseScrollWorkCall(Set<String> indexNames) {
		this.indexNames = indexNames;
	}

	@Override
	protected String summary() {
		return "scroll.close() work execution on indexes '" + indexNames + "'";
	}

	public CallBehavior<Void> verify(CloseScrollWorkCall actualCall) {
		assertThat( actualCall.indexNames )
				.as( "CloseScroll work did not target the expected indexes: " )
				.isEqualTo( indexNames );

		return () -> null;
	}

	@Override
	protected boolean isSimilarTo(CloseScrollWorkCall other) {
		return Objects.equals( indexNames, other.indexNames );
	}
}
