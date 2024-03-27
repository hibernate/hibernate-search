/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.jta.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

@Entity
@Indexed
public class Box extends Container {

	@OneToMany(mappedBy = "box", cascade = { CascadeType.MERGE, CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.PERSIST },
			fetch = FetchType.LAZY)
	@IndexedEmbedded
	private Set<Muffin> muffinSet;

	@OneToMany(mappedBy = "box", cascade = { CascadeType.MERGE, CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.PERSIST },
			fetch = FetchType.LAZY)
	@IndexedEmbedded
	private Set<Doughnut> doughnutSet;

	public Box() {
	}

	/**
	 * @return the color
	 */
	public void addMuffin(Muffin muffin) {
		if ( muffinSet == null ) {
			muffinSet = new HashSet<Muffin>();
		}
		muffinSet.add( muffin );
	}

	/**
	 * @return the muffinSet
	 */
	public Set<Muffin> getMuffinSet() {
		return muffinSet;
	}

	/**
	 * @param muffinSet the muffinSet to set
	 */
	public void setMuffinSet(Set<Muffin> muffinSet) {
		this.muffinSet = muffinSet;
	}

	/**
	 * @return the doughnutSet
	 */
	public Set<Doughnut> getDoughnutSet() {
		return doughnutSet;
	}

	/**
	 * @param doughnutSet the doughnutSet to set
	 */
	public void setDoughnutSet(Set<Doughnut> doughnutSet) {
		this.doughnutSet = doughnutSet;
	}

	public void addDoughnut(Doughnut doughnut) {
		if ( doughnutSet == null ) {
			doughnutSet = new HashSet<Doughnut>();
		}
		doughnutSet.add( doughnut );
	}
}
