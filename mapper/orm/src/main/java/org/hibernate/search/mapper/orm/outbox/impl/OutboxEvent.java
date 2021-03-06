/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.util.Arrays;
import java.util.Objects;
import javax.persistence.Transient;

import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.common.serialization.spi.SerializationUtils;

public final class OutboxEvent {

	public enum Type {
		ADD, ADD_OR_UPDATE, DELETE
	}

	private Integer id;
	private String entityName;
	private String serializedId;
	private byte[] serializedRoutes;
	private Type type;

	@Transient
	private Object identifier;

	public OutboxEvent() {
	}

	public OutboxEvent(String entityName, String serializedId, DocumentRoutesDescriptor routesDescriptor, Type type,
			Object identifier) {
		this.entityName = entityName;
		this.serializedId = serializedId;
		this.serializedRoutes = SerializationUtils.serialize( routesDescriptor );
		this.type = type;
		this.identifier = identifier;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public String getSerializedId() {
		return serializedId;
	}

	public void setSerializedId(String serializedId) {
		this.serializedId = serializedId;
	}

	public byte[] getSerializedRoutes() {
		return serializedRoutes;
	}

	public void setSerializedRoutes(byte[] serializedRoutes) {
		this.serializedRoutes = serializedRoutes;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Object getIdentifier() {
		return identifier;
	}

	public void setIdentifier(Object identifier) {
		this.identifier = identifier;
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
		return type == that.type && Objects.equals( entityName, that.entityName ) && Objects.equals(
				serializedId, that.serializedId ) && Arrays.equals( serializedRoutes, that.serializedRoutes );
	}

	@Override
	public int hashCode() {
		int result = Objects.hash( type, entityName, serializedId );
		result = 31 * result + Arrays.hashCode( serializedRoutes );
		return result;
	}

	@Override
	public String toString() {
		return "OutboxEvent{" +
				"type=" + type +
				", entityName='" + entityName + '\'' +
				", serializedId='" + serializedId + '\'' +
				'}';
	}
}
