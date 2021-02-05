/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;

final class EntityIdDocumentIdMapping
		extends SingleTypeLoadingMapping {

	@Override
	protected String describe() {
		return "document-id-is-entity-id";
	}

	@Override
	public void configure(SimpleSessionFactoryBuilder builder, SingleTypeLoadingModel<?> model) {
		builder.addAnnotatedClasses( model.getIndexedClass(), model.getContainedClass() );
		builder.setProperty( HibernateOrmMapperSettings.MAPPING_CONFIGURER,
				(HibernateOrmSearchMappingConfigurer) context -> {
					TypeMappingStep indexedEntity = context.programmaticMapping().type( model.getIndexedClass() );
					indexedEntity.indexed();
					indexedEntity.property( "containedEager" ).indexedEmbedded();
					indexedEntity.property( "containedLazy" ).indexedEmbedded();
				} );
	}

	@Override
	public boolean isCacheLookupSupported() {
		return true;
	}

	@Override
	public String getDocumentIdForEntityId(int id) {
		return String.valueOf( id );
	}

	@Override
	public Integer generateUniquePropertyForEntityId(int id) {
		// The unique property is not used in this mapping
		return null;
	}

}
