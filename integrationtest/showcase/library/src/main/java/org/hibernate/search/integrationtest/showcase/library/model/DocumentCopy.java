/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

/**
 * A concrete copy of a document, be it physical or dematerialized, that can be borrowed.
 *
 * @param <D> The type of document.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class DocumentCopy<D extends Document<?>> extends AbstractEntity<Integer> {

	@Id
	@GeneratedValue
	private Integer id;

	@ManyToOne(targetEntity = Document.class)
	@IndexedEmbedded
	private D document;

	@ManyToOne
	@IndexedEmbedded(includeDepth = 1)
	private Library library;

	@OneToMany(mappedBy = "copy")
	@OrderBy("id")
	private List<Borrowal> borrowals = new ArrayList<>();

	@Override
	public Integer getId() {
		return id;
	}

	public D getDocument() {
		return document;
	}

	public void setDocument(D document) {
		this.document = document;
	}

	public Library getLibrary() {
		return library;
	}

	public void setLibrary(Library library) {
		this.library = library;
	}

	public List<Borrowal> getBorrowals() {
		return borrowals;
	}

	public void setBorrowals(List<Borrowal> borrowals) {
		this.borrowals = borrowals;
	}

	@Override
	protected String getDescriptionForToString() {
		return "document=" + document + ",library=" + library;
	}
}
