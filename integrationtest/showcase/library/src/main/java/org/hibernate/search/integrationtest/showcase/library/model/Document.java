/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.integrationtest.showcase.library.bridge.annotation.MultiKeywordStringBridge;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Document<C extends DocumentCopy<?>> {

	@Id
	@DocumentId
	private Integer id;

	@Basic
	@Field(analyzer = "default")
	private String title;

	@Basic
	@Field(analyzer = "default")
	private String summary;

	/**
	 * Comma-separated tags.
	 */
	@Basic
	@MultiKeywordStringBridge(fieldName = "tags")
	private String tags;

	@OneToMany(mappedBy = "document", targetEntity = DocumentCopy.class)
	@IndexedEmbedded(includePaths = {"library.location", "library.services"}, storage = ObjectFieldStorage.NESTED)
	private List<C> copies = new ArrayList<>();

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != Hibernate.getClass( o ) ) {
			return false;
		}
		Document<?> document = (Document<?>) o;
		return Objects.equals( id, document.getId() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( getClass() );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( id )
				.append( "]" )
				.toString();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public List<C> getCopies() {
		return copies;
	}

	public void setCopies(List<C> copies) {
		this.copies = copies;
	}

}
