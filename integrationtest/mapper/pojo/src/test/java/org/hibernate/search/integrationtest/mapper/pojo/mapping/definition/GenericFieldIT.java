/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Rule;
import org.junit.Test;

public class GenericFieldIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void searchable() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			Integer id;
			Long searchable;
			LocalDate unsearchable;
			BigDecimal useDefault;
			String implicit;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@GenericField(searchable = Searchable.YES)
			public Long getSearchable() {
				return searchable;
			}

			@GenericField(searchable = Searchable.NO)
			public LocalDate getUnsearchable() {
				return unsearchable;
			}

			@GenericField(searchable = Searchable.DEFAULT)
			public BigDecimal getUseDefault() {
				return useDefault;
			}

			@GenericField
			public String getImplicit() {
				return implicit;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "searchable", Long.class, f -> f.searchable( Searchable.YES ) )
				.field( "unsearchable", LocalDate.class, f -> f.searchable( Searchable.NO ) )
				.field( "useDefault", BigDecimal.class )
				.field( "implicit", String.class )
		);
		setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}
}
