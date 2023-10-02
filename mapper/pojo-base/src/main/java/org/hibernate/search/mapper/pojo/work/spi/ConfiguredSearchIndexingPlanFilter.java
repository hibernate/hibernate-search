/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.util.Set;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface ConfiguredSearchIndexingPlanFilter {

	boolean isIncluded(PojoRawTypeIdentifier<?> typeIdentifier);

	default boolean supportsAsyncProcessing() {
		return true;
	}

	static ConfiguredSearchIndexingPlanFilter create(
			Set<PojoRawTypeIdentifier<?>> includes,
			Set<PojoRawTypeIdentifier<?>> excludes
	) {
		if ( includes.isEmpty() ) {
			return ExcludeAll.INSTANCE;
		}
		if ( excludes.isEmpty() ) {
			return IncludeAll.INSTANCE;
		}
		return new Filter( includes );
	}

	class IncludeAll implements ConfiguredSearchIndexingPlanFilter {

		public static final IncludeAll INSTANCE = new IncludeAll();

		private IncludeAll() {
		}

		@Override
		public boolean isIncluded(PojoRawTypeIdentifier<?> typeIdentifier) {
			return true;
		}

		@Override
		public boolean supportsAsyncProcessing() {
			// cannot work with outbox polling since this filter would allow for all events to be persisted,
			// but then an application level filter might think otherwise when the events will be processed.
			return false;
		}
	}

	class ExcludeAll implements ConfiguredSearchIndexingPlanFilter {

		static final ExcludeAll INSTANCE = new ExcludeAll();

		private ExcludeAll() {
		}

		@Override
		public boolean isIncluded(PojoRawTypeIdentifier<?> typeIdentifier) {
			return false;
		}

	}

	class Filter implements ConfiguredSearchIndexingPlanFilter {
		protected final Set<PojoRawTypeIdentifier<?>> includes;

		private Filter(Set<PojoRawTypeIdentifier<?>> includes) {
			this.includes = includes;
		}

		@Override
		public boolean isIncluded(PojoRawTypeIdentifier<?> typeIdentifier) {
			return includes.contains( typeIdentifier );
		}

		@Override
		public boolean supportsAsyncProcessing() {
			return false;
		}

	}

}
