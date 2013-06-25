/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.query.dsl.impl;

import java.util.Set;

import org.hibernate.search.SearchException;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.EntityContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.util.impl.ScopedAnalyzer;

/**
 * Assuming connection with the search factory
 *
 * @author Emmanuel Bernard
 */
public class ConnectedQueryContextBuilder implements QueryContextBuilder {
	private final SearchFactoryImplementor factory;

	public ConnectedQueryContextBuilder(SearchFactoryImplementor factory) {
		this.factory = factory;
	}

	@Override
	public EntityContext forEntity(Class<?> entityType) {
		return new HSearchEntityContext(entityType, factory );
	}

	public final class HSearchEntityContext implements EntityContext {
		private final ScopedAnalyzer queryAnalyzer;
		private final QueryBuildingContext context;

		public HSearchEntityContext(Class<?> entityType, SearchFactoryImplementor factory) {
			// get a type for meta-data retrieval; if the given type itself is not indexed, one indexed sub-type will
			// be used; note that this allows to e.g. query for fields not present on the given type but on one of its
			// sub-types, but we accept this for now
			Class<?> indexBoundType = getIndexBoundType( entityType, factory );

			if ( indexBoundType == null ) {
				throw new SearchException( String.format( "Can't build query for type %s which is"
						+ " neither indexed nor has any indexed sub-types.",
						entityType.getCanonicalName() ) );
			}

			queryAnalyzer = new ScopedAnalyzer();
			queryAnalyzer.setGlobalAnalyzer( factory.getAnalyzer( indexBoundType ) );
			context = new QueryBuildingContext( factory, queryAnalyzer, indexBoundType );
		}

		/**
		 * Returns the given type itself if it is indexed, otherwise the first found indexed sub-type.
		 *
		 * @param entityType the type of interest
		 * @param factory search factory
		 * @return the given type itself if it is indexed, otherwise the first found indexed sub-type or {@code null} if
		 * neither the given type nor any of its sub-types are indexed
		 */
		private Class<?> getIndexBoundType(Class<?> entityType, SearchFactoryImplementor factory) {
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
			queryAnalyzer.addScopedAnalyzer( field, factory.getAnalyzer( analyzerName ) );
			return this;
		}

		@Override
		public QueryBuilder get() {
			return new ConnectedQueryBuilder(context);
		}
	}
}
