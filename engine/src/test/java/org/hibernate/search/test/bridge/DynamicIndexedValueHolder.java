/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.bridge;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2013 Red Hat Inc.
 */
@Indexed(index = "all")
public class DynamicIndexedValueHolder {

	@DocumentId
	public final String id;

	@Field(norms = Norms.NO, store = Store.YES, analyze = Analyze.YES, name = "value")
	@FieldBridge(impl = MultiFieldMapBridge.class)
	private Map<String, String> values = new HashMap<String, String>();

	public DynamicIndexedValueHolder(String id) {
		this.id = id;
	}

	public DynamicIndexedValueHolder property(String key, String value) {
		values.put( key, value );
		return this;
	}

}
