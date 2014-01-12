/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.indexes.serialization.spi;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo.IndexOptions;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public class LuceneNumericFieldContext {

	private final FieldType field;
	private final String fieldName;
	private final float fieldBoost;

	public LuceneNumericFieldContext(FieldType field, String fieldName, float fieldBoost) {
		this.field = field;
		this.fieldName = fieldName;
		this.fieldBoost = fieldBoost;
	}

	public String getName() {
		return fieldName;
	}

	public int getPrecisionStep() {
		return field.numericPrecisionStep();
	}

	public SerializableStore getStore() {
		return field.stored() ? SerializableStore.YES : SerializableStore.NO;
	}

	public boolean isIndexed() {
		return field.indexed();
	}

	public float getBoost() {
		return fieldBoost;
	}

	public boolean getOmitNorms() {
		return field.omitNorms();
	}

	public boolean getOmitTermFreqAndPositions() {
		return field.indexOptions() == IndexOptions.DOCS_ONLY;
	}

}
