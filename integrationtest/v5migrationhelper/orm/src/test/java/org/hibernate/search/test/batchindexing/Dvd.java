/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Indexed(index = "DVDS")
@Entity
public class Dvd implements TitleAble {

	private long unusuallyNamedIdentifier;
	private String title;
	private Nation firstPublishedIn;

	@Id
	@GeneratedValue
	public long getUnusuallyNamedIdentifier() {
		return unusuallyNamedIdentifier;
	}

	public void setUnusuallyNamedIdentifier(long unusuallyNamedIdentifier) {
		this.unusuallyNamedIdentifier = unusuallyNamedIdentifier;
	}

	@Override
	@Field
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	public Nation getFirstPublishedIn() {
		return firstPublishedIn;
	}

	@Override
	public void setFirstPublishedIn(Nation firstPublishedIn) {
		this.firstPublishedIn = firstPublishedIn;
	}

}
