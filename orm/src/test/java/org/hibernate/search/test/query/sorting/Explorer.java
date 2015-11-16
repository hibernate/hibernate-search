/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.sorting;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;

@Entity
@Indexed
@ClassBridge(impl = Explorer.FirstAndMiddleNamesFieldBridge.class)
public class Explorer {

	@Id
	private int id;

	@Field
	@SortableField
	private int exploredCountries;

	@Field(bridge = @FieldBridge(impl = Explorer.LastNameFieldBridge.class))
	@ElementCollection
	private final Map<String, String> nameParts = new HashMap<>();

	public Explorer() {
	}

	public Explorer(int id) {
		this.id = id;
	}

	public Explorer(int id, int exploredCountries, String firstName, String middleName, String lastName) {
		this.id = id;

		this.exploredCountries = exploredCountries;
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

	/**
	 * Used as class-level bridge for creating the "firstName" and "middleName" document and doc value fields.
	 */
	public static class FirstAndMiddleNamesFieldBridge implements MetadataProvidingFieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			Explorer explorer = (Explorer) value;

			String firstName = explorer.getNameParts().get( "firstName" );
			luceneOptions.addFieldToDocument( "firstName", firstName, document );
			document.add( new SortedDocValuesField( "firstName", new BytesRef( firstName ) ) );

			String middleName = explorer.getNameParts().get( "middleName" );
			luceneOptions.addFieldToDocument( "middleName", middleName, document );
			document.add( new SortedDocValuesField( "middleName", new BytesRef( middleName ) ) );
		}

		@Override
		public Set<String> getSortableFieldNames() {
			Set<String> sortableFields = new HashSet<>();
			sortableFields.add( "firstName" );
			sortableFields.add( "middleName" );

			return sortableFields;
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

			luceneOptions.addFieldToDocument( "lastName", lastName, document );
			document.add( new SortedDocValuesField( "lastName", new BytesRef( lastName ) ) );
		}

		@Override
		public Set<String> getSortableFieldNames() {
			return Collections.singleton( "lastName" );
		}
	}
}
