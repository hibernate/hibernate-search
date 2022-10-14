/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

public final class StubIndexScaleWork implements ToStringTreeAppendable {

	public enum Type {
		MERGE_SEGMENTS, PURGE, FLUSH, REFRESH
	}

	public static Builder builder(Type type) {
		return new Builder( type );
	}

	private final Type type;
	private final String tenantIdentifier;
	private final List<String> routingKeys;

	private StubIndexScaleWork(Builder builder) {
		this.type = builder.type;
		this.tenantIdentifier = builder.tenantIdentifier;
		this.routingKeys = Collections.unmodifiableList( new ArrayList<>( builder.routingKeys ) );
	}

	public Type getType() {
		return type;
	}

	public String getTenantIdentifier() {
		return tenantIdentifier;
	}

	public List<String> getRoutingKeys() {
		return routingKeys;
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
		builder.attribute( "routingKeys", routingKeys );
	}

	public static class Builder {

		private final Type type;
		private String tenantIdentifier;
		private final List<String> routingKeys = new ArrayList<>();

		private Builder(Type type) {
			this.type = type;
		}

		public Builder tenantIdentifier(String tenantIdentifier) {
			this.tenantIdentifier = tenantIdentifier;
			return this;
		}

		public Builder routingKeys(Set<String> routingKeys) {
			this.routingKeys.addAll( routingKeys );
			return this;
		}

		public StubIndexScaleWork build() {
			return new StubIndexScaleWork( this );
		}
	}


}
