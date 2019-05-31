/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index;

public final class StubIndexScopeWork {

	public enum Type {
		OPTIMIZE, PURGE, FLUSH
	}

	public static Builder builder(Type type) {
		return new Builder( type );
	}

	private final Type type;
	private final String tenantIdentifier;

	private StubIndexScopeWork(Builder builder) {
		this.type = builder.type;
		this.tenantIdentifier = builder.tenantIdentifier;
	}

	public Type getType() {
		return type;
	}

	public String getTenantIdentifier() {
		return tenantIdentifier;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "type=" + type
				+ ", tenantIdentifier=" + tenantIdentifier
				+ "]";
	}

	public static class Builder {

		private final Type type;
		private String tenantIdentifier;

		private Builder(Type type) {
			this.type = type;
		}

		public Builder tenantIdentifier(String tenantIdentifier) {
			this.tenantIdentifier = tenantIdentifier;
			return this;
		}

		public StubIndexScopeWork build() {
			return new StubIndexScopeWork( this );
		}
	}


}
