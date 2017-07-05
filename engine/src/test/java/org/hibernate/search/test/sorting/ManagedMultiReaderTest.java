/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.sorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.SortableFieldMetadata;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.filter.impl.FullTextFilterImpl;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.engine.impl.SortConfigurations;
import org.hibernate.search.reader.impl.ManagedMultiReader;
import org.hibernate.search.reader.impl.MultiReaderFactory;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test to verify that the uninverting reader is only applied if actually needed.
 *
 * @author Gunnar Morling
 */
@Category(SkipOnElasticsearch.class) // This test is specific to Lucene
public class ManagedMultiReaderTest {

	private static final IndexedTypeIdentifier PERSON_TYPE = new PojoIndexedTypeIdentifier( Person.class );
	private static final IndexedTypeIdentifier CUSTOMER_TYPE = new PojoIndexedTypeIdentifier( Customer.class );

	@Rule
	public SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Person.class, Customer.class );

	@Test
	public void testStandardReaderIsUsedIfAllSortsAreCovered() throws Exception {
		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		EntityIndexBinding binding = integrator.getIndexBinding( PERSON_TYPE );

		Set<IndexManager> indexManagers = binding.getIndexManagerSelector().forFilters( new FullTextFilterImpl[0] );

		Sort sort = new Sort(
				new SortField( "ageForIntSorting", Type.INT ),
				new SortField( "ageForStringSorting", Type.STRING )
		);

		SortConfigurations configuredSorts = new SortConfigurations.Builder()
			.setIndex( "test" )
			.setEntityType( PERSON_TYPE )
			.addSortableFields(
					Arrays.asList(
							new SortableFieldMetadata.Builder( "ageForIntSorting" ).build(),
							new SortableFieldMetadata.Builder( "ageForStringSorting" ).build()
					)
			)
			.build();

		IndexManager[] indexManagersArray = indexManagers.toArray( new IndexManager[indexManagers.size()] );

		try ( ManagedMultiReader reader = (ManagedMultiReader) MultiReaderFactory.openReader( configuredSorts, sort, indexManagersArray, false ) ) {
			List<? extends IndexReader> actualReaders = reader.getSubReaders();
			assertThat( actualReaders ).hasSize( 1 );
			assertThat( actualReaders.get( 0 ).getClass().getSimpleName() ).isEqualTo( "StandardDirectoryReader" );
		}
	}

	@Test
	public void testUninvertingReaderIsUsedIfNotAllSortsAreCovered() throws Exception {
		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		EntityIndexBinding binding = integrator.getIndexBinding( PERSON_TYPE );

		Set<IndexManager> indexManagers = binding.getIndexManagerSelector().forFilters( new FullTextFilterImpl[0] );

		Sort sort = new Sort(
				new SortField( "ageForIntSorting", Type.INT ),
				new SortField( "ageForStringSorting", Type.STRING )
		);

		SortConfigurations configuredSorts = new SortConfigurations.Builder()
			.setIndex( "person" )
			.setEntityType( PERSON_TYPE )
			.addSortableFields(
					Arrays.asList(
							new SortableFieldMetadata.Builder( "ageForStringSorting" ).build()
					)
			)
			.build();

		IndexManager[] indexManagersArray = indexManagers.toArray( new IndexManager[indexManagers.size()] );

		try ( ManagedMultiReader reader = (ManagedMultiReader) MultiReaderFactory.openReader( configuredSorts, sort, indexManagersArray, true ) ) {
			List<? extends IndexReader> actualReaders = reader.getSubReaders();
			assertThat( actualReaders ).hasSize( 1 );
			assertThat( actualReaders.get( 0 ).getClass().getSimpleName() ).isEqualTo( "UninvertingDirectoryReader" );
		}
	}

	@Test
	public void testCombinationOfStandardAndUninvertingReaderAsRequiredToSortOnInvolvedIndexes() throws Exception {
		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();

		EntityIndexBinding binding = integrator.getIndexBinding( PERSON_TYPE );
		List<IndexManager> indexManagers = new ArrayList<>( binding.getIndexManagerSelector().forFilters( new FullTextFilterImpl[0] ) );

		binding = integrator.getIndexBinding( CUSTOMER_TYPE );
		indexManagers.addAll( binding.getIndexManagerSelector().forFilters( new FullTextFilterImpl[0] ) );

		Sort sort = new Sort(
				new SortField( "ageForIntSorting", Type.INT ),
				new SortField( "ageForStringSorting", Type.STRING )
		);

		SortConfigurations configuredSorts = new SortConfigurations.Builder()
			.setIndex( "person" )
				.setEntityType( PERSON_TYPE )
					.addSortableFields(
							Arrays.asList( new SortableFieldMetadata.Builder( "ageForStringSorting" ).build() )
					)
			.setIndex( "customer" )
				.setEntityType( CUSTOMER_TYPE )
					.addSortableFields(
							Arrays.asList(
									new SortableFieldMetadata.Builder( "ageForStringSorting" ).build(),
									new SortableFieldMetadata.Builder( "ageForIntSorting" ).build()
							)
					)
			.build();

		try ( ManagedMultiReader reader = (ManagedMultiReader) MultiReaderFactory.openReader(
				configuredSorts,
				sort,
				indexManagers.toArray( new IndexManager[indexManagers.size()] ),
				true ) ) {
			List<? extends IndexReader> actualReaders = reader.getSubReaders();
			assertThat( actualReaders ).hasSize( 2 );
			assertThat( actualReaders.get( 0 ).getClass().getSimpleName() ).isEqualTo( "UninvertingDirectoryReader" );
			assertThat( actualReaders.get( 1 ).getClass().getSimpleName() ).isEqualTo( "StandardDirectoryReader" );
		}
	}

	@Indexed(index = "person")
	private class Person {

		@DocumentId
		int id;
	}

	@Indexed(index = "customer")
	private class Customer {

		@DocumentId
		int id;
	}
}
