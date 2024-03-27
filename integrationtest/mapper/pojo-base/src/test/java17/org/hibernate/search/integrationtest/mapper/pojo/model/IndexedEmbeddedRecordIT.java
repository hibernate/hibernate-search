/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.model;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


class IndexedEmbeddedRecordIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock(
			MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@BeforeEach
	void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "myRecord", b2 -> b2
						.field( "text", String.class, b3 -> b3.analyzerName( AnalyzerNames.DEFAULT ) )
						.field( "integer", Integer.class )
						.field( "keywords", String.class, b3 -> b3.multiValued( true ) )
				)
		);

		mapping = setupHelper.start()
				.setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.id = 1;
			MyRecord myRecord = new MyRecord( "someText", 42, List.of( "someText2", "someText3" ) );

			entity1.myRecord = myRecord;

			session.indexingPlan().add( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "myRecord", b2 -> b2
									.field( "text", myRecord.text )
									.field( "integer", myRecord.integer )
									.field( "keywords", myRecord.keywords.get( 0 ) )
									.field( "keywords", myRecord.keywords.get( 1 ) )
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
		private MyRecord myRecord;
	}

	public record MyRecord(@FullTextField String text, @GenericField Integer integer,
							@KeywordField List<String> keywords) {
	}

}
