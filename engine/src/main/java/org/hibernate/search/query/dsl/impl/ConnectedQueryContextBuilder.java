/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.analyzer.impl.AnalyzerReference;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.dsl.EntityContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.util.impl.ScopedAnalyzer;
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
		return new HSearchEntityContext(entityType, factory );
	}

	public final class HSearchEntityContext implements EntityContext {
		private final ScopedAnalyzer queryAnalyzer;
		private final QueryBuildingContext context;

		public HSearchEntityContext(Class<?> entityType, ExtendedSearchIntegrator factory) {
			// get a type for meta-data retrieval; if the given type itself is not indexed, one indexed sub-type will
			// be used; note that this allows to e.g. query for fields not present on the given type but on one of its
			// sub-types, but we accept this for now
			Class<?> indexBoundType = getIndexBoundType( entityType, factory );

			if ( indexBoundType == null ) {
				throw log.cantQueryUnindexedType( entityType.getCanonicalName() );
			}

			Analyzer analyzer = factory.getAnalyzer( indexBoundType );
			final AnalyzerReference reference = new AnalyzerReference();
			reference.setAnalyzer( analyzer );
			queryAnalyzer = new ScopedAnalyzer( reference );
			context = new QueryBuildingContext( factory, reference, indexBoundType );
		}

		/**
		 * Returns the given type itself if it is indexed, otherwise the first found indexed sub-type.
		 *
		 * @param entityType the type of interest
		 * @param factory search factory
		 * @return the given type itself if it is indexed, otherwise the first found indexed sub-type or {@code null} if
		 * neither the given type nor any of its sub-types are indexed
		 */
		private Class<?> getIndexBoundType(Class<?> entityType, ExtendedSearchIntegrator factory) {
			if ( factory.getIndexBinding( entityType ) != null ) {
				return entityType;
			}

			Set<Class<?>> indexedSubTypes = factory.getIndexedTypesPolymorphic( new Class<?>[] { entityType } );

			if ( !indexedSubTypes.isEmpty() ) {
				return indexedSubTypes.iterator().next();
			}

			return null;
		}

		@Override
		public EntityContext overridesForField(String field, String analyzerName) {
			Analyzer analyzer = factory.getAnalyzer( analyzerName );
			AnalyzerReference reference = new AnalyzerReference();
			reference.setAnalyzer( analyzer );
			queryAnalyzer.addScopedAnalyzer( field, reference );
			return this;
		}

		@Override
		public QueryBuilder get() {
			return new ConnectedQueryBuilder(context);
		}
	}
}
