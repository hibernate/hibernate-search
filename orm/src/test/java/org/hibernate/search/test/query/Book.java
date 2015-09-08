/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import org.apache.lucene.document.Document;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.SortableFields;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed(index = "Book")
public class Book {

	private Integer id;
	private String body;
	private String summary;
	private Set<Author> authors = new HashSet<Author>();
	private Author mainAuthor;
	private Date publicationDate;

	@IndexedEmbedded
	@ManyToOne
	public Author getMainAuthor() {
		return mainAuthor;
	}

	public void setMainAuthor(Author mainAuthor) {
		this.mainAuthor = mainAuthor;
	}

	@ManyToMany
	public Set<Author> getAuthors() {
		return authors;
	}

	public void setAuthors(Set<Author> authors) {
		this.authors = authors;
	}

	public Book() {
	}

	public Book(Integer id, String summary, String body) {
		this.id = id;
		this.summary = summary;
		this.body = body;
	}

	@Field
	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Id
	@DocumentId
	@Field(
		name = "id_forIntegerSort",
		store = Store.NO,
		index = Index.NO,
		// TODO explicit bridge is used as workaround for HSEARCH-1987
		bridge = @org.hibernate.search.annotations.FieldBridge(impl = MyIntegerBridge.class)
	)
	@SortableFields({
			@SortableField(forField = "id"),
			@SortableField(forField = "id_forIntegerSort")
	})
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Fields({
			@Field(store = Store.YES),
			@Field(name = "summary_forSort", analyze = Analyze.NO, store = Store.YES)
	})
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@SortableField
	@Field(analyze = Analyze.NO, store = Store.YES)
	@DateBridge(resolution = Resolution.SECOND, encoding = EncodingType.STRING)
	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}

	public static class MyIntegerBridge implements FieldBridge, TwoWayStringBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			luceneOptions.addNumericFieldToDocument( name, value, document );
		}

		@Override
		public String objectToString(Object object) {
			return object.toString();
		}

		@Override
		public Object stringToObject(String stringValue) {
			return Integer.valueOf( stringValue );
		}
	}
}
