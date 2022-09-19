/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexedentities;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;
import java.util.Optional;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SearchMappingIndexedEntitiesIT {

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	public void indexedEntities() {
		//tag::indexedEntities[]
		SearchMapping mapping = /* ... */ // <1>
				//end::indexedEntities[]
				Search.mapping( entityManagerFactory );
		//tag::indexedEntities[]
		SearchIndexedEntity<Book> bookEntity = mapping.indexedEntity( Book.class ); // <2>
		String jpaName = bookEntity.jpaName(); // <3>
		IndexManager indexManager = bookEntity.indexManager(); // <4>
		Backend backend = indexManager.backend(); // <5>
		//end::indexedEntities[]
		assertThat( jpaName ).isEqualTo( "Book" );
		//tag::indexedEntities[]

		SearchIndexedEntity<?> bookEntity2 = mapping.indexedEntity( "Book" ); // <6>
		Class<?> javaClass = bookEntity2.javaClass();
		//end::indexedEntities[]
		assertThat( javaClass ).isEqualTo( Book.class );
		//tag::indexedEntities[]

		for ( SearchIndexedEntity<?> entity : mapping.allIndexedEntities() ) { // <7>
			//end::indexedEntities[]
			assertThat( entity.jpaName() ).isEqualTo( "Book" );
			//tag::indexedEntities[]
			// ...
		}
		//end::indexedEntities[]
	}

	@Test
	public void indexMetamodel() {
		SearchMapping mapping = Search.mapping( entityManagerFactory );
		//tag::indexMetamodel[]
		SearchIndexedEntity<Book> bookEntity = mapping.indexedEntity( Book.class ); // <1>
		IndexManager indexManager = bookEntity.indexManager(); // <2>
		IndexDescriptor indexDescriptor = indexManager.descriptor(); // <3>

		indexDescriptor.field( "releaseDate" ).ifPresent( field -> { // <4>
			String path = field.absolutePath(); // <5>
			String relativeName = field.relativeName();
			// Etc.
			//end::indexMetamodel[]
			assertThat( path ).isEqualTo( "releaseDate" );
			assertThat( relativeName ).isEqualTo( "releaseDate" );
			//tag::indexMetamodel[]

			if ( field.isValueField() ) { // <6>
				IndexValueFieldDescriptor valueField = field.toValueField(); // <7>

				IndexValueFieldTypeDescriptor type = valueField.type(); // <8>
				boolean projectable = type.projectable();
				Class<?> dslArgumentClass = type.dslArgumentClass();
				Class<?> projectedValueClass = type.projectedValueClass();
				Optional<String> analyzerName = type.analyzerName();
				Optional<String> searchAnalyzerName = type.searchAnalyzerName();
				Optional<String> normalizerName = type.normalizerName();
				// Etc.
				//end::indexMetamodel[]
				assertThat( projectable ).isEqualTo( BackendConfiguration.isElasticsearch() ? true : false );
				assertThat( dslArgumentClass ).isEqualTo( Date.class );
				assertThat( projectedValueClass ).isEqualTo( Date.class );
				assertThat( analyzerName ).isEmpty();
				assertThat( searchAnalyzerName ).isEmpty();
				assertThat( normalizerName ).isEmpty();
				//tag::indexMetamodel[]
			}
			else if ( field.isObjectField() ) { // <9>
				IndexObjectFieldDescriptor objectField = field.toObjectField();

				IndexObjectFieldTypeDescriptor type = objectField.type();
				boolean nested = type.nested();
				// Etc.
			}
		} );
		//end::indexMetamodel[]
	}

}
