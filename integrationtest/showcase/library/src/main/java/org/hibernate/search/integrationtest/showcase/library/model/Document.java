/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.integrationtest.showcase.library.analysis.LibraryAnalyzers;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.integrationtest.showcase.library.bridge.annotation.MultiKeywordStringBridge;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

/**
 * Information about a document (book, video, ...) that can be available in a library catalog.
 *
 * @param <C> The type of document copies.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Document<C extends DocumentCopy<?>> extends AbstractEntity<Integer> {

	@Id
	private Integer id;

	@Basic
	@FullTextField(analyzer = LibraryAnalyzers.ANALYZER_DEFAULT)
	@KeywordField(
			name = "title_sort",
			normalizer = LibraryAnalyzers.NORMALIZER_SORT,
			sortable = Sortable.YES
	)
	private String title;

	@GenericField( projectable = Projectable.YES, sortable = Sortable.YES)
	private String author;

	@Basic
	@FullTextField(analyzer = LibraryAnalyzers.ANALYZER_DEFAULT)
	private String summary;

	/**
	 * Comma-separated tags.
	 */
	@Basic
	@MultiKeywordStringBridge(fieldName = "tags")
	private String tags;

	@OneToMany(mappedBy = "document", targetEntity = DocumentCopy.class)
	@IndexedEmbedded(includePaths = {"medium", "library.location", "library.services"}, storage = ObjectFieldStorage.NESTED)
	private List<C> copies = new ArrayList<>();

	public Document() {
	}

	public Document(int id, String title, String author, String summary, String tags) {
		this.id = id;
		this.title = title;
		this.author = author;
		this.summary = summary;
		this.tags = tags;
	}

	@Override
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

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
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

	@Override
	protected String getDescriptionForToString() {
		return getTitle();
	}
}
