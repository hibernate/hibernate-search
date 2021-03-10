/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.util.Arrays;
import java.util.Objects;

public class OutboxEventRetry implements OutboxEventBase {

	private Integer id;

	private String entityName;
	private String entityId;
	private byte[] documentRoutes;

	private int retries = 0;

	public OutboxEventRetry() {
	}

	public OutboxEventRetry(OutboxEventReference eventReference, byte[] documentRoutes) {
		this.entityName = eventReference.getEntityName();
		this.entityId = eventReference.getEntityId();
		this.documentRoutes = documentRoutes;
	}

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public Type getType() {
		return Type.ADD_OR_UPDATE;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	@Override
	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	@Override
	public byte[] getDocumentRoutes() {
		return documentRoutes;
	}

	public void setDocumentRoutes(byte[] documentRoutes) {
		this.documentRoutes = documentRoutes;
	}

	public int getRetries() {
		return retries;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		OutboxEventRetry that = (OutboxEventRetry) o;
		return Objects.equals( entityName, that.entityName ) && Objects.equals(
				entityId, that.entityId ) && Arrays.equals( documentRoutes, that.documentRoutes );
	}

	@Override
	public int hashCode() {
		int result = Objects.hash( entityName, entityId );
		result = 31 * result + Arrays.hashCode( documentRoutes );
		return result;
	}

	@Override
	public String toString() {
		return "OutboxEventRetry{" +
				"id=" + id +
				", entityName='" + entityName + '\'' +
				", entityId='" + entityId + '\'' +
				", documentRoutes=" + Arrays.toString( documentRoutes ) +
				", retries=" + retries +
				'}';
	}
}
