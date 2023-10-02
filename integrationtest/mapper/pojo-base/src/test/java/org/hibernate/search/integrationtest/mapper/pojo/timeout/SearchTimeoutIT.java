/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SearchTimeoutIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@BeforeEach
	void setup() {
		backendMock.expectSchema( INDEX_NAME, b -> b.field( "keyword", String.class ) );
		mapping = setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void truncateAfter() {
		try ( SearchSession session = mapping.createSession() ) {
			SearchQuery<EntityReference> query = session.search( IndexedEntity.class )
					.selectEntityReference()
					.where( f -> f.matchAll() )
					// define a timeout
					.truncateAfter( 5L, TimeUnit.SECONDS )
					.toQuery();

			backendMock.expectSearchReferences( Collections.singletonList( INDEX_NAME ),
					// timeout is supposed to be set on the backend
					b -> b.truncateAfter( 5L, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.of( 0L, Collections.emptyList() )
			);

			assertThat( query.fetchAll().hits() ).isEmpty();
		}
	}

	@Test
	void failAfter() {
		try ( SearchSession session = mapping.createSession() ) {
			SearchQuery<EntityReference> query = session.search( IndexedEntity.class )
					.selectEntityReference()
					.where( f -> f.matchAll() )
					// define a timeout
					.failAfter( 5L, TimeUnit.SECONDS )
					.toQuery();

			SearchException timeoutException = new SearchException( "Timed out" );

			backendMock.expectSearchReferences( Collections.singletonList( INDEX_NAME ),
					// timeout is supposed to be set on the backend
					b -> b.failAfter( 5L, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			assertThatThrownBy( () -> query.fetchAll() )
					.isSameAs( timeoutException );
		}
	}

	@Indexed(index = INDEX_NAME)
	public static final class IndexedEntity {
		@DocumentId
		private Integer id;
		@KeywordField
		private String keyword;
	}
}
