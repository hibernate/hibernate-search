/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public final class StubIndexScaleWork implements ToStringTreeAppendable {

	public enum Type {
		MERGE_SEGMENTS, PURGE, FLUSH, REFRESH
	}

	public static Builder builder(Type type) {
		return new Builder( type );
	}

	private final Type type;
	private final Set<String> tenantIdentifiers;
	private final List<String> routingKeys;

	private StubIndexScaleWork(Builder builder) {
		this.type = builder.type;
		this.tenantIdentifiers = Collections.unmodifiableSet( builder.tenantIdentifiers );
		this.routingKeys = Collections.unmodifiableList( new ArrayList<>( builder.routingKeys ) );
	}

	public Type getType() {
		return type;
	}

	public Set<String> getTenantIdentifiers() {
		return tenantIdentifiers;
	}

	public List<String> getRoutingKeys() {
		return routingKeys;
	}

	@Override
	public String toString() {
		return toStringTree();
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "class", getClass().getSimpleName() );
		appender.attribute( "type", type );
		appender.attribute( "tenantIdentifiers", tenantIdentifiers );
		appender.attribute( "routingKeys", routingKeys );
	}

	public static class Builder {

		private final Type type;
		private final Set<String> tenantIdentifiers = new HashSet<>();
		private final List<String> routingKeys = new ArrayList<>();

		private Builder(Type type) {
			this.type = type;
		}

		public Builder tenantIdentifier(String tenantIdentifier) {
			this.tenantIdentifiers.add( tenantIdentifier );
			return this;
		}

		public Builder tenantIdentifiers(Set<String> tenantIdentifiers) {
			this.tenantIdentifiers.addAll( tenantIdentifiers );
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
