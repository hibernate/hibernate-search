/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.filter.impl;

import java.util.Collections;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.filter.spi.PojoAutomaticIndexingTypeFilter;
import org.hibernate.search.mapper.pojo.automaticindexing.filter.spi.PojoAutomaticIndexingTypeFilterHolder;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public abstract class HibernateOrmAutomaticIndexingTypeFilter implements PojoAutomaticIndexingTypeFilter {

	public static HibernateOrmAutomaticIndexingTypeFilter create(
			PojoAutomaticIndexingTypeFilterHolder fallback
	) {
		return new FilterWithFallback( fallback, Collections.emptySet(), Collections.emptySet() );
	}

	public static HibernateOrmAutomaticIndexingTypeFilter create(
			PojoAutomaticIndexingTypeFilterHolder fallback,
			Set<PojoRawTypeIdentifier<?>> includes,
			Set<PojoRawTypeIdentifier<?>> excludes,
			boolean allTypesProcessed
	) {
		if ( allTypesProcessed ) {
			if ( includes.isEmpty() ) {
				return ExcludeAll.INSTANCE;
			}
			if ( excludes.isEmpty() ) {
				return IncludeAll.INSTANCE;
			}
			return new FilterNoFallback( excludes );
		}

		return fallback == null ?
				new FilterNoFallback( excludes ) :
				new FilterWithFallback( fallback, includes, excludes );
	}

	public abstract boolean supportsEventQueue();

	static class IncludeAll extends HibernateOrmAutomaticIndexingTypeFilter {

		static IncludeAll INSTANCE = new IncludeAll();

		@Override
		public boolean isIncluded(PojoRawTypeIdentifier<?> typeIdentifier) {
			return true;
		}

		@Override
		public boolean supportsEventQueue() {
			// cannot work with outbox polling since this filter would allow for all events to be persisted,
			// but then an application level filter might think otherwise when the events will be processed.
			return false;
		}
	}

	private static class ExcludeAll extends HibernateOrmAutomaticIndexingTypeFilter {

		static ExcludeAll INSTANCE = new ExcludeAll();

		@Override
		public boolean isIncluded(PojoRawTypeIdentifier<?> typeIdentifier) {
			return false;
		}

		@Override
		public boolean supportsEventQueue() {
			return true;
		}
	}

	private static class FilterNoFallback extends HibernateOrmAutomaticIndexingTypeFilter {
		protected final Set<PojoRawTypeIdentifier<?>> excludes;

		private FilterNoFallback(Set<PojoRawTypeIdentifier<?>> excludes) {
			this.excludes = excludes;
		}

		@Override
		public boolean isIncluded(PojoRawTypeIdentifier<?> typeIdentifier) {
			return !excludes.contains( typeIdentifier );
		}

		public boolean supportsEventQueue() {
			return false;
		}

	}

	private static class FilterWithFallback extends HibernateOrmAutomaticIndexingTypeFilter {
		private final PojoAutomaticIndexingTypeFilterHolder fallback;
		protected final Set<PojoRawTypeIdentifier<?>> includes;
		protected final Set<PojoRawTypeIdentifier<?>> excludes;

		private FilterWithFallback(PojoAutomaticIndexingTypeFilterHolder fallback,
				Set<PojoRawTypeIdentifier<?>> includes, Set<PojoRawTypeIdentifier<?>> excludes) {
			this.fallback = fallback;
			this.includes = includes;
			this.excludes = excludes;
		}

		@Override
		public boolean isIncluded(PojoRawTypeIdentifier<?> typeIdentifier) {
			if ( excludes.contains( typeIdentifier ) ) {
				return false;
			}

			if ( includes.contains( typeIdentifier ) ) {
				return true;
			}

			return fallback.filter().isIncluded( typeIdentifier );
		}

		public boolean supportsEventQueue() {
			return false;
		}

	}
}
