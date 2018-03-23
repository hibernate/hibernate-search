/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MapperFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public final class HibernateOrmMetatadaContributor implements MetadataContributor {
	private final MapperFactory<PojoTypeMetadataContributor, ?> mapperFactory;
	private final HibernateOrmBootstrapIntrospector introspector;
	private final Metadata metadata;

	public HibernateOrmMetatadaContributor(MapperFactory<PojoTypeMetadataContributor, ?> mapperFactory,
			HibernateOrmBootstrapIntrospector introspector, Metadata metadata) {
		this.mapperFactory = mapperFactory;
		this.introspector = introspector;
		this.metadata = metadata;
	}

	@Override
	public void contribute(BuildContext buildContext, MetadataCollector collector) {
		// Ensure all entities are declared as such
		for ( PersistentClass persistentClass : metadata.getEntityBindings() ) {
			Class<?> clazz = persistentClass.getMappedClass();
			// getMappedClass() can return null, which should be ignored
			if ( clazz != null ) {
				PojoRawTypeModel<?> typeModel = introspector.getTypeModel( clazz );
				collector.collectContributor(
						mapperFactory, typeModel,
						new HibernateOrmEntityTypeMetadataContributor()
				);
			}
		}
	}
}
