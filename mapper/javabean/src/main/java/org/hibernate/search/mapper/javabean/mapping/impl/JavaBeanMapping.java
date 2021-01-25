/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.mapper.javabean.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.javabean.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.javabean.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.scope.SearchScope;
import org.hibernate.search.mapper.javabean.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.javabean.session.SearchSessionBuilder;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSearchSession;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSearchSessionMappingContext;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSessionIndexedTypeContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class JavaBeanMapping extends AbstractPojoMappingImplementor<SearchMapping>
		implements CloseableSearchMapping, JavaBeanSearchSessionMappingContext, EntityReferenceFactory<EntityReference> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JavaBeanTypeContextContainer typeContextContainer;

	private SearchIntegration integration;

	JavaBeanMapping(PojoMappingDelegate mappingDelegate, JavaBeanTypeContextContainer typeContextContainer) {
		super( mappingDelegate );
		this.typeContextContainer = typeContextContainer;
	}

	@Override
	public void close() {
		if ( integration != null ) {
			integration.close();
		}
	}

	@Override
	public EntityReferenceFactory<EntityReference> entityReferenceFactory() {
		return this;
	}

	@Override
	public EntityReference createEntityReference(String typeName, Object identifier) {
		JavaBeanSessionIndexedTypeContext<?> typeContext =
				typeContextContainer.indexedForEntityName( typeName );
		if ( typeContext == null ) {
			throw new AssertionFailure(
					"Type name " + typeName + " refers to an unknown type"
			);
		}
		return new EntityReferenceImpl( typeContext.typeIdentifier(), typeContext.name(), identifier );
	}

	@Override
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> targetedTypes) {
		return createScope( targetedTypes );
	}

	@Override
	public SearchMapping toConcreteType() {
		return this;
	}

	@Override
	public SearchSession createSession() {
		return createSearchManagerBuilder().build();
	}

	@Override
	public SearchSessionBuilder createSessionWithOptions() {
		return createSearchManagerBuilder();
	}

	@Override
	public <T> SearchScopeImpl<T> createScope(Collection<? extends Class<? extends T>> classes) {
		List<PojoRawTypeIdentifier<? extends T>> typeIdentifiers = new ArrayList<>( classes.size() );
		for ( Class<? extends T> clazz : classes ) {
			typeIdentifiers.add( PojoRawTypeIdentifier.of( clazz ) );
		}

		// Explicit type parameter is necessary here for ECJ (Eclipse compiler)
		return new SearchScopeImpl<T>( delegate().createPojoScope( this, typeIdentifiers,
				typeContextContainer::indexedForExactType ) );
	}

	@Override
	public <E> SearchIndexedEntity<E> indexedEntity(Class<E> entityType) {
		SearchIndexedEntity<E> type = typeContextContainer.indexedForExactClass( entityType );
		if ( type == null ) {
			throw log.notIndexedEntityType( entityType );
		}
		return type;
	}

	@Override
	public SearchIndexedEntity<?> indexedEntity(String entityName) {
		SearchIndexedEntity<?> type = typeContextContainer.indexedForEntityName( entityName );
		if ( type == null ) {
			throw log.notIndexedEntityName( entityName );
		}
		return type;
	}

	@Override
	public Collection<SearchIndexedEntity<?>> allIndexedEntities() {
		return Collections.unmodifiableCollection( typeContextContainer.allIndexed() );
	}

	public void setIntegration(SearchIntegration integration) {
		this.integration = integration;
	}

	private SearchSessionBuilder createSearchManagerBuilder() {
		return new JavaBeanSearchSession.Builder(
				this, typeContextContainer
		);
	}
}
