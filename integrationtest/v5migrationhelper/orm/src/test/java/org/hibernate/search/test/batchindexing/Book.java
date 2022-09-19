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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;

@Indexed
@Entity
public class Book implements TitleAble {

	private long id;

	private String title;

	private Nation firstPublishedIn;

	@Id
	@GeneratedValue
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@Fetch(FetchMode.SELECT)
	@IndexedEmbedded(depth = 3)
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	public Nation getFirstPublishedIn() {
		return firstPublishedIn;
	}

	@Override
	public void setFirstPublishedIn(Nation firstPublishedIn) {
		this.firstPublishedIn = firstPublishedIn;
	}

}
