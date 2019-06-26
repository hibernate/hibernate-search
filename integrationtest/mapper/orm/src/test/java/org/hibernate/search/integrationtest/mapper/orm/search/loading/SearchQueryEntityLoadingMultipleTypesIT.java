/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import java.util.Arrays;

import org.hibernate.SessionFactory;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.EntityIdDocumentIdIndexedEntity;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.NonEntityIdDocumentIdIndexedEntity;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test entity loading when executing a search query
 * for cases involving multiple entity types.
 */
public class SearchQueryEntityLoadingMultipleTypesIT extends AbstractSearchQueryEntityLoadingIT {

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( EntityIdDocumentIdIndexedEntity.NAME );
		backendMock.expectAnySchema( NonEntityIdDocumentIdIndexedEntity.NAME );

		sessionFactory = ormSetupHelper.withBackendMock( backendMock )
				.setup(
						EntityIdDocumentIdIndexedEntity.class,
						NonEntityIdDocumentIdIndexedEntity.class
				);

		backendMock.verifyExpectationsMet();

		initData();
	}

	/**
	 * Test loading two entity types with a different document ID mapping:
	 * one mapped to the entity ID, the other to a property.
	 */
	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3203", "HSEARCH-3349" })
	public void mixedDocumentIdMapping_entityIdAndProperty() {
		testLoading(
				Arrays.asList(
						EntityIdDocumentIdIndexedEntity.class,
						NonEntityIdDocumentIdIndexedEntity.class
				),
				Arrays.asList(
						EntityIdDocumentIdIndexedEntity.NAME,
						NonEntityIdDocumentIdIndexedEntity.NAME
				),
				c -> c
						.doc( NonEntityIdDocumentIdIndexedEntity.NAME, "42" )
						.doc( EntityIdDocumentIdIndexedEntity.NAME, "2" ),
				c -> c
						.entity( NonEntityIdDocumentIdIndexedEntity.class, 1 )
						.entity( EntityIdDocumentIdIndexedEntity.class, 2 )
		);
	}

	@Override
	protected SessionFactory sessionFactory() {
		return sessionFactory;
	}

	private void initData() {
		// We don't care about what is indexed exactly, so use the lenient mode
		backendMock.inLenientMode( () ->
				OrmUtils.withinTransaction( sessionFactory, session -> {
					session.persist( new NonEntityIdDocumentIdIndexedEntity( 1, 42 ) );
					session.persist( new EntityIdDocumentIdIndexedEntity( 2 ) );
				} )
		);
	}

}
