/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;
import org.hibernate.search.engine.impl.AnalyzerRegistry;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.query.dsl.EntityContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Assuming connection with the search factory
 *
 * @author Emmanuel Bernard
 */
public class ConnectedQueryContextBuilder implements QueryContextBuilder {

	private static final Log log = LoggerFactory.make();

	private final ExtendedSearchIntegrator factory;

	public ConnectedQueryContextBuilder(ExtendedSearchIntegrator factory) {
		this.factory = factory;
	}

	@Override
	public EntityContext forEntity(Class<?> entityType) {
		IndexedTypeIdentifier realtype = new PojoIndexedTypeIdentifier( entityType );
		return new HSearchEntityContext( realtype, factory );
	}

	public final class HSearchEntityContext implements EntityContext {
		private final IndexedTypeIdentifier indexBoundType;
		private final ScopedAnalyzerReference originalAnalyzerReference;
		private final ScopedAnalyzerReference.CopyBuilder queryAnalyzerReferenceBuilder;
		private final AnalyzerRegistry analyzerRegistry;

		public HSearchEntityContext(IndexedTypeIdentifier entityType, ExtendedSearchIntegrator factory) {
			// get a type for meta-data retrieval; if the given type itself is not indexed, one indexed sub-type will
			// be used; note that this allows to e.g. query for fields not present on the given type but on one of its
			// sub-types, but we accept this for now
			indexBoundType = getIndexBoundType( entityType, factory );

			if ( indexBoundType == null ) {
				IndexedTypeSet configuredSubTypes = factory.getConfiguredTypesPolymorphic( entityType.asTypeSet() );
				if ( configuredSubTypes.isEmpty() ) {
					throw log.cantQueryUnconfiguredType( entityType.getName() );
				}
				else {
					throw log.cantQueryUnindexedType( entityType.getName() );
				}
			}

			originalAnalyzerReference = factory.getAnalyzerReference( indexBoundType );
			queryAnalyzerReferenceBuilder = factory.getAnalyzerReference( indexBoundType ).startCopy();
			IndexManagerType indexManagerType = factory.getIndexBinding( indexBoundType ).getIndexManagerType();
			analyzerRegistry = factory.getIntegration( indexManagerType ).getAnalyzerRegistry();
		}

		/**
		 * Returns the given type itself if it is indexed, otherwise the first found indexed sub-type.
		 *
		 * @param entityType the type of interest
		 * @param factory search factory
		 * @return the given type itself if it is indexed, otherwise the first found indexed sub-type or {@code null} if
		 * neither the given type nor any of its sub-types are indexed
		 */
		private IndexedTypeIdentifier getIndexBoundType(IndexedTypeIdentifier entityType, ExtendedSearchIntegrator factory) {
			if ( factory.getIndexBinding( entityType ) != null ) {
				return entityType;
			}

			IndexedTypeSet indexedSubTypes = factory.getIndexedTypesPolymorphic( entityType.asTypeSet() );

			if ( !indexedSubTypes.isEmpty() ) {
				return indexedSubTypes.iterator().next();
			}

			return null;
		}

		@Override
		public EntityContext overridesForField(String field, String analyzerName) {
			queryAnalyzerReferenceBuilder.addAnalyzerReference( field, analyzerRegistry.getAnalyzerReference( analyzerName ) );
			return this;
		}

		@Override
		public QueryBuilder get() {
			return new ConnectedQueryBuilder( new QueryBuildingContext( factory, originalAnalyzerReference, queryAnalyzerReferenceBuilder.build(), indexBoundType ) );
		}
	}
}
