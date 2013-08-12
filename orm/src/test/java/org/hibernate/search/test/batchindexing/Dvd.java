/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.batchindexing;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

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
