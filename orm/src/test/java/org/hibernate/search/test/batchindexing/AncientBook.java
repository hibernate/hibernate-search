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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;

import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class AncientBook extends Book {

	public String catalogueGroupName = "";
	public Set<String> alternativeTitles = new HashSet<String>();

	public String getCatalogueGroupName() {
		return catalogueGroupName;
	}

	public void setCatalogueGroupName(String catalogueGroupName) {
		this.catalogueGroupName = catalogueGroupName;
	}

	@ElementCollection(fetch = FetchType.EAGER)
	public Set<String> getAlternativeTitles() {
		return alternativeTitles;
	}

	public void setAlternativeTitles(Set<String> alternativeTitles) {
		this.alternativeTitles = alternativeTitles;
	}

}
