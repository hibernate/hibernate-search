/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;

final class NonEntityIdDocumentIdMapping
		extends SingleTypeLoadingMapping {

	@Override
	protected String describe() {
		return "document-id-is-NOT-entity-id";
	}

	@Override
	public void configure(SimpleSessionFactoryBuilder builder, SingleTypeLoadingModel<?> model) {
		builder.addAnnotatedClasses( model.getIndexedClass(), model.getContainedClass() );
		builder.setProperty(
				HibernateOrmMapperSettings.MAPPING_CONFIGURER,
				(HibernateOrmSearchMappingConfigurer) context -> {
					TypeMappingStep indexedEntity = context.programmaticMapping().type( model.getIndexedClass() );
					indexedEntity.indexed();
					indexedEntity.property( "uniqueProperty" ).documentId();
					indexedEntity.property( "containedEager" ).indexedEmbedded();
					indexedEntity.property( "containedLazy" ).indexedEmbedded();
				}
		);
	}

	@Override
	public boolean isCacheLookupSupported() {
		return false;
	}

	@Override
	public String getDocumentIdForEntityId(int id) {
		return String.valueOf( generateUniquePropertyForEntityId( id ) );
	}

	@Override
	public Integer generateUniquePropertyForEntityId(int id) {
		// Use a different document ID than the ID, to check that Search uses the right value when loading
		return id + 40;
	}
}
