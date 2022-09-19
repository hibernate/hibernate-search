/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.jta.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

// Hibernate
@Entity
@Table(name = "muffin")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = "muffin")
public class Muffin {

	@Id()
	private Long muffinId;

	@ManyToOne
	private Box box;

	@Column(length = 255, nullable = false)
	@GenericField
	private String kind;

	public Muffin() {
	}

	/**
	 * @return the muffinId
	 */
	public Long getMuffinId() {
		return muffinId;
	}

	/**
	 * @param muffinId the muffinId to set
	 */
	public void setMuffinId(Long muffinId) {
		this.muffinId = muffinId;
	}

	/**
	 * @return the box
	 */
	public Box getBox() {
		return box;
	}

	/**
	 * @param box the box to set
	 */
	public void setBox(Box box) {
		this.box = box;
	}

	/**
	 * @return the kind
	 */
	public String getKind() {
		return kind;
	}

	/**
	 * @param kind the kind to set
	 */
	public void setKind(String kind) {
		this.kind = kind;
	}

}
