/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index;

import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public final class StubSchemaManagementWork implements ToStringTreeAppendable {

	public enum Type {
		CREATE_IF_MISSING,
		CREATE_OR_VALIDATE,
		CREATE_OR_UPDATE,
		DROP_IF_EXISTING,
		DROP_AND_CREATE,
		VALIDATE
	}

	public static Builder builder(Type type) {
		return new Builder( type );
	}

	private final Type type;

	private StubSchemaManagementWork(Builder builder) {
		this.type = builder.type;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return toStringTree();
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "class", getClass().getSimpleName() );
		appender.attribute( "type", type );
	}

	public static class Builder {

		private final Type type;

		private Builder(Type type) {
			this.type = type;
		}

		public StubSchemaManagementWork build() {
			return new StubSchemaManagementWork( this );
		}
	}

}
