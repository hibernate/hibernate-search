/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.util.Objects;

final class OutboxEventReference {

	private final String entityName;
	private final String entityId;

	public OutboxEventReference(String entityName, String entityId) {
		this.entityName = entityName;
		this.entityId = entityId;
	}

	public String getEntityId() {
		return entityId;
	}

	public String getEntityName() {
		return entityName;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		OutboxEventReference that = (OutboxEventReference) o;
		return Objects.equals( entityName, that.entityName ) && Objects.equals( entityId, that.entityId );
	}

	@Override
	public int hashCode() {
		return Objects.hash( entityName, entityId );
	}

	@Override
	public String toString() {
		return "OutboxEventReference{" +
				"entityName='" + entityName + '\'' +
				", entityId='" + entityId + '\'' +
				'}';
	}
}
