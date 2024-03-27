/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.model;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class GenericPropertyIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@BeforeEach
	void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "genericProperty", b2 -> b2
						/*
						 * If generics are not handled correctly, these fields will have type "T" or "Object"
						 * and Hibernate Search will fail to resolve the bridge for them
						 */
						.field( "content", String.class )
						.field( "arrayContent", String.class, b3 -> b3.multiValued( true ) )
				)
		);

		mapping = setupHelper.start()
				.setup( IndexedEntity.class, GenericEntity.class );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.id = 1;
			GenericEntity<String> genericEntity = new GenericEntity<>();
			genericEntity.content = "genericEntityContent";
			genericEntity.arrayContent = new String[] { "entry1", "entry2" };

			entity1.genericProperty = genericEntity;
			genericEntity.parent = entity1;

			session.indexingPlan().add( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "genericProperty", b2 -> b2
									.field( "content", genericEntity.content )
									.field( "arrayContent", genericEntity.arrayContent[0] )
									.field( "arrayContent", genericEntity.arrayContent[1] )
							)
					);
		}
	}

	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@DocumentId
		private Integer id;

		@IndexedEmbedded
		@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "parent")))
		private GenericEntity<String> genericProperty;
	}

	public static class GenericEntity<T> {

		private IndexedEntity parent;

		@GenericField
		private T content;

		@GenericField
		private T[] arrayContent;
	}

}
