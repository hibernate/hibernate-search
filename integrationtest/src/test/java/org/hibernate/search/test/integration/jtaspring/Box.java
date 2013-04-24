/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.integration.jtaspring;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Indexed;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = "container")
@Indexed(index = "container")
@Analyzer(impl = StandardAnalyzer.class)
public class Box extends Container {

	@OneToMany(mappedBy = "box", cascade = { CascadeType.MERGE, CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.PERSIST }, fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_Box_Muffins")
	private Set<Muffin> muffinSet;

	@OneToMany(mappedBy = "box", cascade = { CascadeType.MERGE, CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.PERSIST }, fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_Box_Doughnuts")
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
