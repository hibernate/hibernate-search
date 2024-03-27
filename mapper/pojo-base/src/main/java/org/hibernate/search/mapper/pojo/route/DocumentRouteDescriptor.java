/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	@Override
	public String toString() {
		return "DocumentRouteDescriptor{" +
				"routingKey='" + routingKey + '\'' +
				'}';
	}

	public String routingKey() {
		return routingKey;
	}

}
