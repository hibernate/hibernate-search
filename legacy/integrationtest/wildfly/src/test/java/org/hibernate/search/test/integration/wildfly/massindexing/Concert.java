/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.massindexing;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Gunnar Morling
 */
@Entity
@Indexed
public class Concert {

	/**
	 * Used to artificially slow-down the indexing of entities.
	 */
	public static boolean SLOW_DOWN = false;

	private String id;
	private String artist;
	private Date date;

	Concert() {
	}

	public Concert(String artist, Date date) {
		this.artist = artist;
		this.date = date;
	}

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Field(index = Index.YES, store = Store.NO)
	public String getArtist() {
		if ( SLOW_DOWN ) {
			try {
				Thread.sleep( 2 );
			}
			catch (InterruptedException e) {
				throw new RuntimeException( e );
			}
		}

		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	@Field(index = Index.YES, store = Store.NO)
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

}
