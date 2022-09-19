/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Transient;

public final class OutboxEvent {

	public enum Status {
		PENDING, ABORTED
	}

	private UUID id;

	private String entityName;
	private String entityId;
	private int entityIdHash;
	private byte[] payload;
	private int retries = 0;
	private Instant processAfter;
	private Status status = Status.PENDING;

	@Transient
	private Object originalEntityId;

	protected OutboxEvent() {
	}

	public OutboxEvent(String entityName, String entityId, int entityIdHash, byte[] payload,
			Object originalEntityId) {
		this.entityName = entityName;
		this.entityId = entityId;
		this.entityIdHash = entityIdHash;
		this.payload = payload;
		this.processAfter = Instant.now();
		this.originalEntityId = originalEntityId;
	}

	@Override
	public String toString() {
		return "OutboxEvent{" +
				"id=" + id +
				", entityName='" + entityName + '\'' +
				", entityId='" + entityId + '\'' +
				", entityIdHash='" + entityIdHash + '\'' +
				", retries=" + retries +
				", processAfter=" + processAfter +
				", status=" + status +
				", originalEntityId=" + originalEntityId +
				'}';
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public int getEntityIdHash() {
		return entityIdHash;
	}

	public void setEntityIdHash(int entityIdHash) {
		this.entityIdHash = entityIdHash;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public int getRetries() {
		return retries;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public Instant getProcessAfter() {
		return processAfter;
	}

	public void setProcessAfter(Instant processAfter) {
		this.processAfter = processAfter;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Object getOriginalEntityId() {
		return originalEntityId;
	}

	public void setOriginalEntityId(Object originalEntityId) {
		this.originalEntityId = originalEntityId;
	}

	OutboxEventReference getReference() {
		return new OutboxEventReference( getEntityName(), getEntityId() );
	}

}
