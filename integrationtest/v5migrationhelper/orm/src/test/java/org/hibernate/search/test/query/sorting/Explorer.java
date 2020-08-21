/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.sorting;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.apache.lucene.document.Document;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

@Entity
@Indexed
@ClassBridge(
		name = "fn",
		impl = Explorer.FirstAndMiddleNamesFieldBridge.class
)
public class Explorer {

	@Id
	private int id;

	@Field
	@SortableField
	private int exploredCountries;

	@Field(bridge = @FieldBridge(impl = Explorer.LastNameFieldBridge.class))
	@ElementCollection
	private final Map<String, String> nameParts = new HashMap<>();

	@ManyToOne
	@IndexedEmbedded
	/*
	 * Don't store the id in "favoriteTerritory" directly as this could conflict
	 * with the @IndexedEmbedded with the Elasticsearch backend
	 */
	@Field(name = "favoriteTerritory.idFromBridge", bridge = @FieldBridge(impl = Territory.IdFieldBridge.class))
	private Territory favoriteTerritory;

	public Explorer() {
	}

	public Explorer(int id) {
		this.id = id;
	}

	public Explorer(int id, int exploredCountries, Territory favoriteTerritory, String firstName, String middleName, String lastName) {
		this.id = id;

		this.exploredCountries = exploredCountries;
		this.favoriteTerritory = favoriteTerritory;

		nameParts.put( "firstName", firstName );
		nameParts.put( "middleName", middleName );
		nameParts.put( "lastName", lastName );
	}

	public int getId() {
		return id;
	}

	public int getExploredCountries() {
		return exploredCountries;
	}

	public Map<String, String> getNameParts() {
		return nameParts;
	}

	public Territory getFavoriteTerritory() {
		return favoriteTerritory;
	}

	/**
	 * Used as class-level bridge for creating the "firstName" and "middleName" document and doc value fields.
	 */
	public static class FirstAndMiddleNamesFieldBridge implements MetadataProvidingFieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			Explorer explorer = (Explorer) value;

			String firstName = explorer.getNameParts().get( "firstName" );
			luceneOptions.addFieldToDocument( name + "_firstName", firstName, document );
			luceneOptions.addSortedDocValuesFieldToDocument( name + "_firstName", firstName, document );

			String middleName = explorer.getNameParts().get( "middleName" );
			luceneOptions.addFieldToDocument( name + "_middleName", middleName, document );
			luceneOptions.addSortedDocValuesFieldToDocument( name + "_middleName", middleName, document );
		}

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			builder
				.field( name + "_firstName", FieldType.STRING )
					.sortable( true )
				.field( name + "_middleName", FieldType.STRING )
					.sortable( true );
		}
	}

	/**
	 * Used as field-level bridge for creating the "lastName" document and doc value fields.
	 */
	public static class LastNameFieldBridge implements MetadataProvidingFieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			@SuppressWarnings("unchecked")
			Map<String, String> nameParts = (Map<String, String>) value;
			String lastName = nameParts.get( "lastName" );

			luceneOptions.addFieldToDocument( name + "_lastName", lastName, document );
			luceneOptions.addSortedDocValuesFieldToDocument( name + "_lastName", lastName, document );
		}

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			builder
				.field( name + "_lastName", FieldType.STRING )
					.sortable( true );
		}
	}
}
