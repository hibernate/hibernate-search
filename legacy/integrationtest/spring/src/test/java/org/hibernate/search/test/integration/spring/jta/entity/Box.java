/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.jta.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Indexed;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = "container")
@Indexed(index = "container")
@Analyzer(impl = StandardAnalyzer.class)
public class Box extends Container {

	@OneToMany(mappedBy = "box", cascade = { CascadeType.MERGE, CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.PERSIST }, fetch = FetchType.LAZY)
	private Set<Muffin> muffinSet;

	@OneToMany(mappedBy = "box", cascade = { CascadeType.MERGE, CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.PERSIST }, fetch = FetchType.LAZY)
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
