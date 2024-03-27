/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.indexedentities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.sql.Date;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.analysis.AnalyzerDescriptor;
import org.hibernate.search.engine.backend.analysis.NormalizerDescriptor;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SearchMappingIndexedEntitiesIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	void indexedEntities() {
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
	void indexMetamodel() {
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
				Set<String> traits = type.traits(); // <9>
				if ( traits.contains( IndexFieldTraits.Aggregations.RANGE ) ) {
					// ...
					//end::indexMetamodel[]
				}
				else {
					fail( "The field should be aggregable!" );
					//tag::indexMetamodel[]
				}
				//end::indexMetamodel[]
				assertThat( traits ).contains(
						IndexFieldTraits.Predicates.EXISTS,
						IndexFieldTraits.Predicates.MATCH,
						IndexFieldTraits.Predicates.RANGE,
						IndexFieldTraits.Aggregations.TERMS,
						IndexFieldTraits.Aggregations.RANGE
				);
				//tag::indexMetamodel[]
			}
			else if ( field.isObjectField() ) { // <10>
				IndexObjectFieldDescriptor objectField = field.toObjectField();

				IndexObjectFieldTypeDescriptor type = objectField.type();
				boolean nested = type.nested();
				// Etc.
			}
		} );

		Collection<? extends AnalyzerDescriptor> analyzerDescriptors = indexDescriptor.analyzers(); // <11>
		for ( AnalyzerDescriptor analyzerDescriptor : analyzerDescriptors ) {
			String analyzerName = analyzerDescriptor.name();
			// ...
		}

		Optional<? extends AnalyzerDescriptor> analyzerDescriptor = indexDescriptor.analyzer( "some-analyzer-name" ); // <12>
		// ...

		Collection<? extends NormalizerDescriptor> normalizerDescriptors = indexDescriptor.normalizers(); // <13>
		for ( NormalizerDescriptor normalizerDescriptor : normalizerDescriptors ) {
			String normalizerName = normalizerDescriptor.name();
			// ...
		}

		Optional<? extends NormalizerDescriptor> normalizerDescriptor = indexDescriptor.normalizer( "some-normalizer-name" ); // <14>
		// ...
		//end::indexMetamodel[]
	}

}
