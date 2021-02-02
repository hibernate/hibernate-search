/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.route;

import java.io.Serializable;
import java.util.Objects;

public final class DocumentRouteDescriptor implements Serializable {

	public static DocumentRouteDescriptor of(String routingKey) {
		return new DocumentRouteDescriptor( routingKey );
	}

	private final String routingKey;

	private DocumentRouteDescriptor(String routingKey) {
		this.routingKey = routingKey;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		DocumentRouteDescriptor that = (DocumentRouteDescriptor) o;
		return Objects.equals( routingKey, that.routingKey );
	}

	@Override
	public int hashCode() {
		return Objects.hash( routingKey );
	}

	public String routingKey() {
		return routingKey;
	}

}
