/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.optimizations;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
public class SongWithLongTitle {

	@Id
	@DocumentId
	private Long id;

	@Field(store = Store.YES)
	private String band;

	@Field(store = Store.YES)
	private String title;

	public String getBand() {
		return band;
	}

	public void setBand(String band) {
		this.band = band;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( band == null ) ? 0 : band.hashCode() );
		result = prime * result + ( ( title == null ) ? 0 : title.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		SongWithLongTitle other = (SongWithLongTitle) obj;
		if ( band == null ) {
			if ( other.band != null ) {
				return false;
			}
		}
		else if ( !band.equals( other.band ) ) {
			return false;
		}
		if ( title == null ) {
			if ( other.title != null ) {
				return false;
			}
		}
		else if ( !title.equals( other.title ) ) {
			return false;
		}
		return true;
	}

}
