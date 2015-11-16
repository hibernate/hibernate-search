/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.engine.metadata.impl.SortableFieldMetadata;

/**
 * Provides information about the sortable fields configured for given entities.
 *
 * @author Gunnar Morling
 */
public class SortConfigurations implements Iterable<SortConfigurations.SortConfiguration> {

	@Override
	public String toString() {
		return configurations.toString();
	}

	private final List<SortConfiguration> configurations;

	private SortConfigurations(List<SortConfiguration> configurations) {
		this.configurations = Collections.unmodifiableList( configurations );
	}

	@Override
	public Iterator<SortConfiguration> iterator() {
		return configurations.iterator();
	}

	/**
	 * Provides information about the sortable fields configured for one or more entities mapped to the same index.
	 *
	 * @author Gunnar Morling
	 */
	public static class SortConfiguration {

		private final String indexName;
		private final Map<Class<?>, List<SortableFieldMetadata>> sortableFieldsByType;

		public SortConfiguration(String indexName, Map<Class<?>, List<SortableFieldMetadata>> sortableFieldsByType) {
			this.indexName = indexName;
			this.sortableFieldsByType = sortableFieldsByType;
		}

		/**
		 * Returns all those sort fields from the given {@link Sort} that cannot be satisfied by the existing sortable
		 * fields (doc value fields) declared for the given indexed type.
		 */
		public List<String> getUncoveredSorts(Class<?> entityType, Sort sort) {
			List<String> uncoveredSorts = new ArrayList<>();

			for ( SortField sortField : sort.getSort() ) {
				// no doc value field needed for these
				if ( sortField.getType() == SortField.Type.DOC || sortField.getType() == SortField.Type.SCORE ) {
					continue;
				}

				boolean isConfigured = false;
				for ( SortableFieldMetadata sortFieldMetadata : sortableFieldsByType.get( entityType ) ) {
					if ( sortFieldMetadata.getFieldName().equals( sortField.getField() ) ) {
						isConfigured = true;
						break;
					}
				}

				if ( !isConfigured ) {
					uncoveredSorts.add( sortField.getField() );
				}
			}

			return uncoveredSorts;
		}

		public Set<Class<?>> getEntityTypes() {
			return sortableFieldsByType.keySet();
		}

		public String getIndexName() {
			return indexName;
		}

		@Override
		public String toString() {
			return "SortConfiguration [indexName=" + indexName + ", sortableFieldsByType=" + sortableFieldsByType + "]";
		}
	}

	/**
	 * Collects the sortable fields configured for given entities.
	 *
	 * @author Gunnar Morling
	 */
	public static class Builder {

		private final Map<String, Map<Class<?>, List<SortableFieldMetadata>>> builtConfigurations = new HashMap<>();
		private Map<Class<?>, List<SortableFieldMetadata>> currentIndexBucket;
		private List<SortableFieldMetadata> currentEntityTypeBucket;

		public Builder setIndex(String indexName) {
			currentIndexBucket = builtConfigurations.get( indexName );

			if ( currentIndexBucket == null ) {
				currentIndexBucket = new HashMap<>();
				builtConfigurations.put( indexName, currentIndexBucket );
			}

			return this;
		}

		public Builder setEntityType(Class<?> entityType) {
			currentEntityTypeBucket = currentIndexBucket.get( entityType );

			if ( currentEntityTypeBucket == null ) {
				currentEntityTypeBucket = new ArrayList<>();
				currentIndexBucket.put( entityType, currentEntityTypeBucket );
			}

			return this;
		}

		public Builder addSortableFields(Collection<SortableFieldMetadata> sortableFieldMetadata) {
			currentEntityTypeBucket.addAll( sortableFieldMetadata );
			return this;
		}

		public SortConfigurations build() {
			List<SortConfiguration> configurations = new ArrayList<>();

			for ( Entry<String, Map<Class<?>, List<SortableFieldMetadata>>> configuration : builtConfigurations.entrySet() ) {
				configurations.add( new SortConfiguration( configuration.getKey(), configuration.getValue() ) );
			}

			return new SortConfigurations( configurations );
		}
	}
}
