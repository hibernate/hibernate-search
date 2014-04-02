/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.test.engine.optimizations;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.ElementCollection;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;

@MappedSuperclass
public class MappedSuperclassWithCollectionField {

	@Id
	@GeneratedValue
	private Long id;

	@ElementCollection
	@Field(bridge = @FieldBridge(impl = CollectionOfStringsFieldBridge.class), analyze = Analyze.NO)
	private Collection<String> collection = new ArrayList<String>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Collection<String> getCollection() {
		return collection;
	}

}
