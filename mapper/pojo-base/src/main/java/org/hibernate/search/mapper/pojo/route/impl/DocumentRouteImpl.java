/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.route.impl;

import java.util.Objects;

import org.hibernate.search.mapper.pojo.route.DocumentRoute;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;

public class DocumentRouteImpl implements DocumentRoute {

	private String routingKey;

	@Override
	public void routingKey(String routingKey) {
		this.routingKey = routingKey;
	}

	public String routingKey() {
		return routingKey;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		DocumentRouteImpl that = (DocumentRouteImpl) o;
		return Objects.equals( routingKey, that.routingKey );
	}

	@Override
	public int hashCode() {
		return Objects.hash( routingKey );
	}

	public DocumentRouteDescriptor toDescriptor() {
		return DocumentRouteDescriptor.of( routingKey );
	}
}
