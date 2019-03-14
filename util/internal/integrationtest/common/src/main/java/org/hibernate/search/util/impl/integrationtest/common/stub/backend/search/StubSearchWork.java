/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StubSearchWork {

	public enum ResultType {
		REFERENCES,
		OBJECTS,
		PROJECTIONS
	}

	public static Builder builder(ResultType resultType) {
		return new Builder( resultType );
	}

	private final ResultType resultType;
	private final List<String> routingKeys;
	private final Long offset;
	private final Long limit;

	private StubSearchWork(Builder builder) {
		this.resultType = builder.resultType;
		this.routingKeys = Collections.unmodifiableList( new ArrayList<>( builder.routingKeys ) );
		this.offset = builder.offset;
		this.limit = builder.limit;
	}

	public ResultType getResultType() {
		return resultType;
	}

	public List<String> getRoutingKeys() {
		return routingKeys;
	}

	public Long getOffset() {
		return offset;
	}

	public Long getLimit() {
		return limit;
	}

	@Override
	public String toString() {
		return "StubSearchWork[" +
				", routingKeys=" + routingKeys +
				", offset=" + offset +
				", limit=" + limit +
				']';
	}

	public static class Builder {

		private final ResultType resultType;
		private final List<String> routingKeys = new ArrayList<>();
		private Long offset;
		private Long limit;

		private Builder(ResultType resultType) {
			this.resultType = resultType;
		}

		public Builder routingKey(String routingKey) {
			this.routingKeys.add( routingKey );
			return this;
		}

		public Builder offset(Long offset) {
			this.offset = offset;
			return this;
		}

		public Builder limit(Long limit) {
			this.limit = limit;
			return this;
		}

		public StubSearchWork build() {
			return new StubSearchWork( this );
		}
	}

}
