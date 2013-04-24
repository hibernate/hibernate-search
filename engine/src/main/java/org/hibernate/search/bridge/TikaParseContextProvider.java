/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.bridge;

import org.apache.tika.parser.ParseContext;

/**
 * @author Hardy Ferentschik
 */
public interface TikaParseContextProvider {

	/**
	 * This method is called by the Tika bridge prior to parsing the data.
	 * <p/>
	 * It allows to create a custom {@code ParseContext}
	 *
	 * @param name the field name of the property which is processed by the Tika bridge
	 * @param value the value to be indexed
	 * @return A {@code ParseContext} instance used by the Tika bridge to parse the data
	 * @see <a href="http://tika.apache.org/1.1/parser.html#apiorgapachetikametadataMetadata.html">Tika API</a>
	 */
	ParseContext getParseContext(String name, Object value);

}
