/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.sorting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.fest.assertions.Assertions;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.CustomTypeMetadata;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeMap;
import org.hibernate.search.spi.impl.IndexedTypeMaps;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.impl.CollectionHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TestForIssue(jiraKey = "HSEARCH-2561")
@Category(SkipOnElasticsearch.class) // This test is specific to Lucene
public class CustomTypeMetadataSortingTest {

	private static final String UNINVERTING_READER_LOG_CODE = "HSEARCH000289";

	private static final IndexedTypeIdentifier PROPERTY_SET_TYPE_ID = new PojoIndexedTypeIdentifier( PropertySet.class );
	private static final IndexedTypeIdentifier PERSON_TYPE_ID = new PojoIndexedTypeIdentifier( Person.class );

	private static final Sort FIRST_NAME_SORT = new Sort( new SortField( "properties.firstName", SortField.Type.STRING ) );
	private static final Sort FIRST_NAME_SORT_REVERSED =
			new Sort( new SortField( "properties.firstName", SortField.Type.STRING, true ) );

	@Rule
	public final ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Rule
	public final SearchFactoryHolder factoryHolder = new SearchFactoryHolder( PropertySet.class, ExtendedPropertySet.class, Person.class )
			.withProperty( Environment.INDEX_UNINVERTING_ALLOWED, "true" );

	private final SearchITHelper helper = new SearchITHelper( factoryHolder );

	@Test
	public void undeclaredSortableField_defaultMetadata() {
		helper.index(
				new PropertySet( 0 )
						.put( "firstName", "Aaron" )
						.put( "lastName", "Zahnd" )
						.put( "nonSortableField", "zzz" ),
				new ExtendedPropertySet( 1 )
						.put( "firstName", "Mike" )
						.put( "lastName", "Myers" )
						.put( "nonSortableField", "mmm" ),
				new Person( 2, "Zach" )
		);

		logged.expectMessage( UNINVERTING_READER_LOG_CODE );

		Query luceneQuery = factoryHolder.getSearchFactory().buildQueryBuilder().forEntity( PropertySet.class ).get().all().createQuery();

		HSQuery query = factoryHolder.getSearchFactory().createHSQuery( luceneQuery, PropertySet.class, Person.class )
				.sort( FIRST_NAME_SORT );

		Assertions.assertThat( query.queryEntityInfos() ).onProperty( "id" ).as( "Sorted IDs" )
				.containsExactly( 0, 1, 2 );

		query.sort( FIRST_NAME_SORT_REVERSED );
		Assertions.assertThat( query.queryEntityInfos() ).onProperty( "id" ).as( "Sorted IDs" )
				.containsExactly( 2, 1, 0 );
	}

	@Test
	public void undeclaredSortableField_incorrectCustomMetadata() {
		helper.index(
				new PropertySet( 0 )
						.put( "firstName", "Aaron" )
						.put( "lastName", "Zahnd" )
						.put( "nonSortableField", "zzz" ),
				new ExtendedPropertySet( 1 )
						.put( "firstName", "Mike" )
						.put( "lastName", "Myers" )
						.put( "nonSortableField", "mmm" ),
				new Person( 2, "Zach" )
		);

		logged.expectMessage( UNINVERTING_READER_LOG_CODE );

		Query luceneQuery = factoryHolder.getSearchFactory().buildQueryBuilder().forEntity( PropertySet.class ).get().all().createQuery();

		CustomTypeMetadata incorrectCustomMetadata = new CustomTypeMetadata() {
			@Override
			public Set<String> getSortableFields() {
				return Collections.singleton( "properties.nonSortableField" );
			}
		};
		IndexedTypeMap<CustomTypeMetadata> metadata = IndexedTypeMaps.hashMap();
		metadata.put( PROPERTY_SET_TYPE_ID, incorrectCustomMetadata );
		metadata.put( PERSON_TYPE_ID, new EmptyMetadata() );
		HSQuery query = factoryHolder.getSearchFactory().createHSQuery( luceneQuery, metadata )
				.sort( FIRST_NAME_SORT );

		Assertions.assertThat( query.queryEntityInfos() ).onProperty( "id" ).as( "Sorted IDs" )
				.containsExactly( 0, 1, 2 );

		query.sort( FIRST_NAME_SORT_REVERSED );
		Assertions.assertThat( query.queryEntityInfos() ).onProperty( "id" ).as( "Sorted IDs" )
				.containsExactly( 2, 1, 0 );
	}

	@Test
	public void undeclaredSortableField_correctCustomMetadata() {
		helper.index(
				new PropertySet( 0 )
						.put( "firstName", "Aaron" )
						.put( "lastName", "Zahnd" )
						.put( "nonSortableField", "zzz" ),
				new ExtendedPropertySet( 1 )
						.put( "firstName", "Mike" )
						.put( "lastName", "Myers" )
						.put( "nonSortableField", "mmm" ),
				new Person( 2, "Zach" )
		);

		// We expect HSearch to *not* use an uninverting reader
		logged.expectMessageMissing( UNINVERTING_READER_LOG_CODE );

		Query luceneQuery = factoryHolder.getSearchFactory().buildQueryBuilder().forEntity( PropertySet.class ).get().all().createQuery();

		IndexedTypeMap<CustomTypeMetadata> metadata = IndexedTypeMaps.hashMap();
		metadata.put( PROPERTY_SET_TYPE_ID, new PropertySetMetadata() );
		metadata.put( PERSON_TYPE_ID, new EmptyMetadata() );
		HSQuery query = factoryHolder.getSearchFactory().createHSQuery( luceneQuery, metadata )
				.sort( FIRST_NAME_SORT );

		Assertions.assertThat( query.queryEntityInfos() ).onProperty( "id" ).as( "Sorted IDs" )
				.containsExactly( 0, 1, 2 );

		query.sort( FIRST_NAME_SORT_REVERSED );
		Assertions.assertThat( query.queryEntityInfos() ).onProperty( "id" ).as( "Sorted IDs" )
				.containsExactly( 2, 1, 0 );
	}

	@Indexed(index = "propertySet")
	private static class PropertySet {

		@DocumentId
		int id;

		@Field(bridge = @FieldBridge(impl = PropertiesBridge.class))
		Map<String, String> properties = new LinkedHashMap<>();

		public PropertySet(int id) {
			this.id = id;
		}

		public PropertySet put(String name, String value) {
			properties.put( name, value );
			return this;
		}
	}

	@Indexed(index = "propertySet")
	private static class ExtendedPropertySet extends PropertySet {
		public ExtendedPropertySet(int id) {
			super( id );
		}
	}

	@Indexed(index = "person")
	private static class Person {
		@DocumentId
		int id;

		@Field(name = "properties.firstName", analyze = Analyze.NO)
		@SortableField(forField = "properties.firstName")
		String firstName;

		public Person(int id, String firstName) {
			this.id = id;
			this.firstName = firstName;
		}
	}

	/**
	 * This bridge adds sortable fields without those being declared in the metadata,
	 * which is why it is useful to be able to override sortable fields when querying.
	 */
	public static class PropertiesBridge implements org.hibernate.search.bridge.FieldBridge {

		public static final Set<String> SORTABLE_PROPERTIES = CollectionHelper.asImmutableSet( new String[] { "firstName", "lastName" } );

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			@SuppressWarnings("unchecked")
			Map<String, String> properties = (Map<String, String>) value;
			for ( Map.Entry<String, String> property : properties.entrySet() ) {
				String fieldName = name + "." + property.getKey();
				luceneOptions.addFieldToDocument( fieldName, property.getValue(), document );
				if ( SORTABLE_PROPERTIES.contains( property.getKey() ) ) {
					luceneOptions.addSortedDocValuesFieldToDocument( fieldName, property.getValue(), document );
				}
			}
		}
	}

	private static class PropertySetMetadata implements CustomTypeMetadata {

		private final Set<String> sortableFields;

		public PropertySetMetadata() {
			Set<String> sortableFields = new TreeSet<>();
			for ( String propertyName : PropertiesBridge.SORTABLE_PROPERTIES ) {
				sortableFields.add( "properties." + propertyName );
			}
			this.sortableFields = Collections.unmodifiableSet( sortableFields );
		}

		@Override
		public Set<String> getSortableFields() {
			return sortableFields;
		}

	}
	private static class EmptyMetadata implements CustomTypeMetadata {

		@Override
		public Set<String> getSortableFields() {
			return Collections.emptySet();
		}

	}
}
