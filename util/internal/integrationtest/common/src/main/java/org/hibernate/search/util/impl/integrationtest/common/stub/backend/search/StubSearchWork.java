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
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

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
	private final Integer offset;
	private final Integer limit;
	private final Long truncateAfterTimeout;
	private final TimeUnit truncateAfterTimeUnit;
	private final Long failAfterTimeout;
	private final TimeUnit failAfterTimeUnit;

	private StubSearchWork(Builder builder) {
		this.resultType = builder.resultType;
		this.routingKeys = Collections.unmodifiableList( new ArrayList<>( builder.routingKeys ) );
		this.offset = builder.offset;
		this.limit = builder.limit;
		this.truncateAfterTimeout = builder.truncateAfterTimeout;
		this.truncateAfterTimeUnit = builder.truncateAfterTimeUnit;
		this.failAfterTimeout = builder.failAfterTimeout;
		this.failAfterTimeUnit = builder.failAfterTimeUnit;
	}

	public ResultType getResultType() {
		return resultType;
	}

	public List<String> getRoutingKeys() {
		return routingKeys;
	}

	public Long getTruncateAfterTimeout() {
		return truncateAfterTimeout;
	}

	public TimeUnit getTruncateAfterTimeUnit() {
		return truncateAfterTimeUnit;
	}

	public Long getFailAfterTimeout() {
		return failAfterTimeout;
	}

	public TimeUnit getFailAfterTimeUnit() {
		return failAfterTimeUnit;
	}

	public Integer getOffset() {
		return offset;
	}

	public Integer getLimit() {
		return limit;
	}

	@Override
	public String toString() {
		return new StringJoiner( ", ", StubSearchWork.class.getSimpleName() + "[", "]" )
				.add( "resultType=" + resultType )
				.add( "routingKeys=" + routingKeys )
				.add( "offset=" + offset )
				.add( "limit=" + limit )
				.add( "truncateAfterTimeout=" + truncateAfterTimeout )
				.add( "truncateAfterTimeUnit=" + truncateAfterTimeUnit )
				.add( "failAfterTimeout=" + failAfterTimeout )
				.add( "failAfterTimeUnit=" + failAfterTimeUnit )
				.toString();
	}

	public static class Builder {

		private final ResultType resultType;
		private final List<String> routingKeys = new ArrayList<>();
		private Long truncateAfterTimeout;
		private TimeUnit truncateAfterTimeUnit;
		private Long failAfterTimeout;
		private TimeUnit failAfterTimeUnit;
		private Integer offset;
		private Integer limit;

		private Builder(ResultType resultType) {
			this.resultType = resultType;
		}

		public Builder routingKey(String routingKey) {
			this.routingKeys.add( routingKey );
			return this;
		}

		public Builder truncateAfter(long timeout, TimeUnit timeUnit) {
			this.truncateAfterTimeout = timeout;
			this.truncateAfterTimeUnit = timeUnit;
			return this;
		}

		public Builder failAfter(long timeout, TimeUnit timeUnit) {
			this.failAfterTimeout = timeout;
			this.failAfterTimeUnit = timeUnit;
			return this;
		}

		public Builder offset(Integer offset) {
			this.offset = offset;
			return this;
		}

		public Builder limit(Integer limit) {
			this.limit = limit;
			return this;
		}

		public StubSearchWork build() {
			return new StubSearchWork( this );
		}
	}

}
