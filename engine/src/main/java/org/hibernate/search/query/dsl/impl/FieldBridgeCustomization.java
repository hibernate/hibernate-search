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

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.bridge.FieldBridge;

/**
 * Customization context which allows to inject a field bridge instance to be used for querying the current field.
 * <p>
 * Not part of the public DSL API for the time being in order to gain experiences with using the functionality first;
 * may be made public in a future release by merging with {@code FieldCustomization}.
 *
 * @author Gunnar Morling
 */
public interface FieldBridgeCustomization<T> {

	/**
	 * Sets the field bridge for querying the current field; any other bridge associated with this field will be ignored
	 * for the query
	 *
	 * @param fieldBridge the field bridge to use
	 * @return this object, following the method chaining pattern
	 */
	T withFieldBridge(FieldBridge fieldBridge);
}
