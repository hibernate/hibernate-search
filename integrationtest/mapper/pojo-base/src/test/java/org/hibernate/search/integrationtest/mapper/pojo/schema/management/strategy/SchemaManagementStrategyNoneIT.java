/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.schema.management.strategy;

import java.lang.invoke.MethodHandles;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.cfg.spi.JavaBeanMapperSpiSettings;
import org.hibernate.search.mapper.javabean.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.javabean.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Rule;
import org.junit.Test;

public class SchemaManagementStrategyNoneIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public final JavaBeanMappingSetupHelper setupHelper
			= JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void none() {
		CloseableSearchMapping mapper = setup();
		// Nothing should have happened
		backendMock.verifyExpectationsMet();

		mapper.close();
		// Nothing should have happened
		backendMock.verifyExpectationsMet();
	}

	protected final CloseableSearchMapping setup() {
		backendMock.expectAnySchema( IndexedEntity1.NAME );
		backendMock.expectAnySchema( IndexedEntity2.NAME );
		return (CloseableSearchMapping) setupHelper.start()
				.withProperty(
						JavaBeanMapperSpiSettings.SCHEMA_MANAGEMENT_STRATEGY,
						getStrategyName()
				)
				.withConfiguration( b -> {
					b.addEntityType( IndexedEntity1.class );
					b.addEntityType( IndexedEntity2.class );
				} )
				.setup( IndexedEntity1.class, IndexedEntity2.class );
	}

	protected String getStrategyName() {
		return SchemaManagementStrategyName.NONE.externalRepresentation();
	}

	@Indexed(index = IndexedEntity1.NAME)
	static class IndexedEntity1 {

		static final String NAME = "indexed1";

		@DocumentId
		private Integer id;
	}

	@Indexed(index = IndexedEntity2.NAME)
	static class IndexedEntity2 {

		static final String NAME = "indexed2";

		@DocumentId
		private Integer id;
	}
}
