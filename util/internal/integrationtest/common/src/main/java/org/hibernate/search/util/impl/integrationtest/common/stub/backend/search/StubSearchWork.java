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
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.search.common.TimeoutStrategy;

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
	private final Long timeout;
	private final TimeUnit timeUnit;
	private final TimeoutStrategy strategy;

	private StubSearchWork(Builder builder) {
		this.resultType = builder.resultType;
		this.routingKeys = Collections.unmodifiableList( new ArrayList<>( builder.routingKeys ) );
		this.offset = builder.offset;
		this.limit = builder.limit;
		this.timeout = builder.timeout;
		this.timeUnit = builder.timeUnit;
		this.strategy = builder.strategy;
	}

	public ResultType getResultType() {
		return resultType;
	}

	public List<String> getRoutingKeys() {
		return routingKeys;
	}

	public Long getTimeout() {
		return timeout;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public Integer getOffset() {
		return offset;
	}

	public Integer getLimit() {
		return limit;
	}

	public TimeoutStrategy getStrategy() {
		return strategy;
	}

	@Override
	public String toString() {
		return "StubSearchWork[" +
				", routingKeys=" + routingKeys +
				", timeout=" + timeout +
				", timeUnit=" + timeUnit +
				", strategy=" + strategy +
				", offset=" + offset +
				", limit=" + limit +
				']';
	}

	public static class Builder {

		private final ResultType resultType;
		private final List<String> routingKeys = new ArrayList<>();
		private Long timeout;
		private TimeUnit timeUnit;
		private TimeoutStrategy strategy;
		private Integer offset;
		private Integer limit;

		private Builder(ResultType resultType) {
			this.resultType = resultType;
		}

		public Builder routingKey(String routingKey) {
			this.routingKeys.add( routingKey );
			return this;
		}

		public Builder timeout(long timeout, TimeUnit timeUnit, TimeoutStrategy strategy) {
			this.timeout = timeout;
			this.timeUnit = timeUnit;
			this.strategy = strategy;
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
