/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.builtin;

import java.net.URI;
import java.sql.Blob;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.TikaBridge;

/**
 * @author Hardy Ferentschik
 */
@Indexed
class Book {

	private Integer id;
	private Blob contentAsBlob;
	private byte[] contentAsBytes;
	private URI contentAsURI;

	private Set<String> contentAsListOfString;

	public Book(int id, String... contents) {
		this( id );
		Set<String> temp = new HashSet<>();
		for ( String string : contents ) {
			temp.add( string );
		}
		this.contentAsListOfString = temp;
	}

	public Book(int id, Blob content) {
		this( id );
		this.contentAsBlob = content;
	}

	public Book(int id, byte[] content) {
		this( id );
		this.contentAsBytes = content;
	}

	public Book(int id, URI content) {
		this( id );
		this.contentAsURI = content;
	}

	public Book(int id) {
		this.id = id;
	}

	@DocumentId
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Field(indexNullAs = "<NULL>")
	@TikaBridge
	public Blob getContentAsBlob() {
		return contentAsBlob;
	}

	public void setContentAsBlob(Blob contentAsBlob) {
		this.contentAsBlob = contentAsBlob;
	}

	@Field(indexNullAs = "<NULL>")
	@TikaBridge
	public byte[] getContentAsBytes() {
		return contentAsBytes;
	}

	public void setContentAsBytes(byte[] contentAsBytes) {
		this.contentAsBytes = contentAsBytes;
	}

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
	public Set<String> getContentAsListOfString() {
		return contentAsListOfString;
	}

	public void setContentAsListOfString(Set<String> contentAsListOfString) {
		this.contentAsListOfString = contentAsListOfString;
	}
}
