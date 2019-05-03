/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index;

import org.hibernate.search.engine.backend.index.spi.DocumentRefreshStrategy;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;

public final class StubIndexWork {

	public enum Type {
		ADD, UPDATE, DELETE, OPTIMIZE, PURGE, FLUSH
	}

	public static Builder builder(Type type) {
		return new Builder( type );
	}

	private final Type type;
	private final String tenantIdentifier;
	private final String identifier;
	private final String routingKey;
	private final DocumentRefreshStrategy refreshStrategy;
	private final StubDocumentNode document;

	private StubIndexWork(Builder builder) {
		this.type = builder.type;
		this.tenantIdentifier = builder.tenantIdentifier;
		this.identifier = builder.identifier;
		this.routingKey = builder.routingKey;
		this.refreshStrategy = builder.refreshStrategy;
		this.document = builder.document;
	}

	public Type getType() {
		return type;
	}

	public String getTenantIdentifier() {
		return tenantIdentifier;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getRoutingKey() {
		return routingKey;
	}

	public DocumentRefreshStrategy getRefreshStrategy() {
		return refreshStrategy;
	}

	public StubDocumentNode getDocument() {
		return document;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "type=" + type
				+ ", tenantIdentifier=" + tenantIdentifier
				+ ", identifier=" + identifier
				+ ", routingKey=" + routingKey
				+ ", refreshStrategy=" + refreshStrategy
				+ ", document=" + document
				+ "]";
	}

	public static class Builder {

		private final Type type;
		private String tenantIdentifier;
		private String identifier;
		private String routingKey;
		private DocumentRefreshStrategy refreshStrategy;
		private StubDocumentNode document;

		private Builder(Type type) {
			this.type = type;
		}

		public Builder tenantIdentifier(String tenantIdentifier) {
			this.tenantIdentifier = tenantIdentifier;
			return this;
		}

		public Builder identifier(String identifier) {
			this.identifier = identifier;
			return this;
		}

		public Builder routingKey(String routingKey) {
			this.routingKey = routingKey;
			return this;
		}

		public Builder refresh(DocumentRefreshStrategy refreshStrategy) {
			this.refreshStrategy = refreshStrategy;
			return this;
		}

		public Builder document(StubDocumentNode document) {
			this.document = document;
			return this;
		}

		public StubIndexWork build() {
			return new StubIndexWork( this );
		}
	}


}
