/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;

public final class StubDocumentWork implements ToStringTreeAppendable {

	public enum Type {
		ADD, ADD_OR_UPDATE, DELETE
	}

	public static Builder builder(Type type) {
		return new Builder( type );
	}

	private final Type type;
	private final String tenantIdentifier;
	private final String identifier;
	private final String routingKey;
	private final Object entityIdentifier;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;
	private final StubDocumentNode document;

	private StubDocumentWork(Builder builder) {
		this.type = builder.type;
		this.tenantIdentifier = builder.tenantIdentifier;
		this.identifier = builder.identifier;
		this.routingKey = builder.routingKey;
		this.entityIdentifier = builder.entityIdentifier;
		this.commitStrategy = builder.commitStrategy;
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

	public Object getEntityIdentifier() {
		return entityIdentifier;
	}

	public String getRoutingKey() {
		return routingKey;
	}

	public DocumentCommitStrategy getCommitStrategy() {
		return commitStrategy;
	}

	public DocumentRefreshStrategy getRefreshStrategy() {
		return refreshStrategy;
	}

	public StubDocumentNode getDocument() {
		return document;
	}

	@Override
	public String toString() {
		return new ToStringTreeBuilder().value( this ).toString();
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "type", type );
		builder.attribute( "tenantIdentifier", tenantIdentifier );
		builder.attribute( "identifier", identifier );
		builder.attribute( "routingKey", routingKey );
		builder.attribute( "commitStrategy", commitStrategy );
		builder.attribute( "refreshStrategy", refreshStrategy );
		builder.attribute( "document", document );
	}

	public static class Builder {

		private final Type type;
		private String tenantIdentifier;
		private String identifier;
		private String routingKey;
		private Object entityIdentifier;
		private DocumentCommitStrategy commitStrategy;
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

		public Builder entityIdentifier(Object entityIdentifier) {
			this.entityIdentifier = entityIdentifier;
			return this;
		}

		public Builder commit(DocumentCommitStrategy commitStrategy) {
			this.commitStrategy = commitStrategy;
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

		public StubDocumentWork build() {
			return new StubDocumentWork( this );
		}
	}


}
