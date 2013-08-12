/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.hibernate.search.query.fieldcache.impl;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldCache;

/**
 * We need a collection of similar implementations, one per each FieldCache.DEFAULT.accessmethod
 * to be able to deal with arrays of primitive values without autoboxing all of them.
 * This particular implementation doesn't do any conversion.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @see FieldLoadingStrategy
 */
public final class StringFieldLoadingStrategy implements FieldLoadingStrategy {
	private final String fieldName;
	private String[] currentCache;

	public StringFieldLoadingStrategy(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public void loadNewCacheValues(IndexReader reader) throws IOException {
		currentCache = FieldCache.DEFAULT.getStrings( reader, fieldName );
	}

	@Override
	public String collect(int relativeDocId) {
		return currentCache[relativeDocId];
	}
}
