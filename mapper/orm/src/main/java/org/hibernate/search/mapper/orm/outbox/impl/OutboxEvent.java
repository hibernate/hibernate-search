/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.util.Arrays;
import java.util.Objects;

import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.common.serialization.spi.SerializationUtils;

public final class OutboxEvent {

	private final String entityName;
	private final String serializedId;
	private final byte[] serializedRoutes;

	public OutboxEvent(String entityName, String serializedId, DocumentRoutesDescriptor routesDescriptor) {
		this.entityName = entityName;
		this.serializedId = serializedId;
		this.serializedRoutes = SerializationUtils.serialize( routesDescriptor );
	}

	public String getEntityName() {
		return entityName;
	}

	public String getSerializedId() {
		return serializedId;
	}

	public byte[] getSerializedRoutes() {
		return serializedRoutes;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		OutboxEvent that = (OutboxEvent) o;
		return Objects.equals( entityName, that.entityName ) && Objects.equals(
				serializedId, that.serializedId ) && Arrays.equals( serializedRoutes, that.serializedRoutes );
	}

	@Override
	public int hashCode() {
		int result = Objects.hash( entityName, serializedId );
		result = 31 * result + Arrays.hashCode( serializedRoutes );
		return result;
	}

	@Override
	public String toString() {
		return "OutboxEvent{" +
				"entityName='" + entityName + '\'' +
				", serializedId='" + serializedId + '\'' +
				", serializedRoutes=" + Arrays.toString( serializedRoutes ) +
				'}';
	}
}
