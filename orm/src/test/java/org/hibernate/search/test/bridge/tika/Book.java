/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.tika;

import java.net.URI;
import java.sql.Blob;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.TikaBridge;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Book {

	private Integer id;
	private Blob contentAsBlob;
	private byte[] contentAsBytes;
	private URI contentAsURI;

	private Set<String> contentAsListOfString;

	public Book() {
	}

	public Book(String... contents) {
		Set<String> temp = new HashSet<>();
		for ( String string : contents ) {
			temp.add( string );
		}
		this.contentAsListOfString = temp;
	}

	public Book(Blob content) {
		this.contentAsBlob = content;
	}

	public Book(byte[] content) {
		this.contentAsBytes = content;
	}

	public Book(URI content) {
		this.contentAsURI = content;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Field(indexNullAs = "<NULL>")
	@TikaBridge
	public Blob getContentAsBlob() {
		return contentAsBlob;
	}

	public void setContentAsBlob(Blob contentAsBlob) {
		this.contentAsBlob = contentAsBlob;
	}

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Field(indexNullAs = "<NULL>")
	@TikaBridge
	public byte[] getContentAsBytes() {
		return contentAsBytes;
	}

	public void setContentAsBytes(byte[] contentAsBytes) {
		this.contentAsBytes = contentAsBytes;
	}

	@Basic(fetch = FetchType.LAZY)
	@Field(indexNullAs = "<NULL>")
	@TikaBridge
	public URI getContentAsURI() {
		return contentAsURI;
	}

	public void setContentAsURI(URI contentAsURI) {
		this.contentAsURI = contentAsURI;
	}

	@IndexedEmbedded
	@Field
	@TikaBridge
	@ElementCollection
	public Set<String> getContentAsListOfString() {
		return contentAsListOfString;
	}

	public void setContentAsListOfString(Set<String> contentAsListOfString) {
		this.contentAsListOfString = contentAsListOfString;
	}
}
