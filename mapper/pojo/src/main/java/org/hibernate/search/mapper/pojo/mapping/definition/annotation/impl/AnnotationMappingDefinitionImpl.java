/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataCollector;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperFactory;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeOrdering;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.model.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;

/**
 * @author Yoann Rodiere
 */
public class AnnotationMappingDefinitionImpl implements AnnotationMappingDefinition, MetadataContributor {

	private final PojoMapperFactory<?> mapperFactory;

	private final PojoIntrospector introspector;

	private final Set<Class<?>> annotatedTypes = new HashSet<>();

	public AnnotationMappingDefinitionImpl(PojoMapperFactory<?> mapperFactory, PojoIntrospector introspector) {
		this.mapperFactory = mapperFactory;
		this.introspector = introspector;
	}

	@Override
	public AnnotationMappingDefinition add(Class<?> annotatedType) {
		this.annotatedTypes.add( annotatedType );
		return this;
	}

	@Override
	public AnnotationMappingDefinition add(Set<Class<?>> annotatedTypes) {
		this.annotatedTypes.addAll( annotatedTypes );
		return this;
	}

	@Override
	public void contribute(BuildContext buildContext, TypeMetadataCollector collector) {
		BeanResolver beanResolver = buildContext.getServiceManager().getBeanResolver();
		annotatedTypes.stream()
				// Take super types into account
				// Note: the order of super types (ascending or descending) does not matter here, we just pick one order
				.flatMap( PojoTypeOrdering.get()::getDescendingSuperTypes )
				.distinct()
				// Just for performance, exclude types that we know are not annotated
				.filter( Predicate.isEqual( Object.class ).negate() )
				// TODO filter out other types, e.g. standard Java interfaces such as Serializable?
				.forEach( annotatedType -> {
					IndexedTypeIdentifier typeId = new PojoIndexedTypeIdentifier( annotatedType );

					TypeModel<?> typeModel = introspector.getTypeModel( annotatedType );
					String indexName = typeModel.getAnnotationByType( Indexed.class )
							.map( Indexed::index ).orElse( null );

					collector.collect( mapperFactory, typeId, indexName,
							new AnnotationPojoTypeNodeMetadataContributorImpl( beanResolver, typeModel ) );
				} );
	}

}
