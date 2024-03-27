/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Immutable;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


// This does not work because Hibernate ORM doesn't support instantiating records
// for entities/embeddables at the moment.
@Disabled
class IndexedEmbeddedRecordIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "myRecord", b2 -> b2
						.field( "text", String.class, b3 -> b3.analyzerName( AnalyzerNames.DEFAULT ) )
						.field( "integer", Integer.class )
						.field( "keywords", String.class, b3 -> b3.multiValued( true ) )
				)
		);

		sessionFactory = ormSetupHelper.start()
				.setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.id = 1;
			MyRecord myRecord = new MyRecord( "someText", 42, List.of( "someText2", "someText3" ) );

			entity1.myRecord = myRecord;

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "myRecord", b2 -> b2
									.field( "text", myRecord.text )
									.field( "integer", myRecord.integer )
									.field( "keywords", myRecord.keywords.get( 0 ) )
									.field( "keywords", myRecord.keywords.get( 1 ) )
							)
					);
		} );
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Embedded
		@IndexedEmbedded
		private MyRecord myRecord;
	}

	@Immutable
	@Embeddable
	public record MyRecord(@FullTextField String text, @GenericField Integer integer,
							@ElementCollection @KeywordField List<String> keywords) {
	}

}
