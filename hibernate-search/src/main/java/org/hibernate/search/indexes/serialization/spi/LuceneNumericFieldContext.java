/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
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
package org.hibernate.search.indexes.serialization.spi;

import org.apache.lucene.document.NumericField;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class LuceneNumericFieldContext {
	private NumericField field;

	public LuceneNumericFieldContext(NumericField numericField) {
		this.field = numericField;
	}

	public String getName() {
		return field.name();
	}

	public int getPrecisionStep() {
		return field.getPrecisionStep();
	}

	public SerializableStore getStore() {
		return field.isStored() ? SerializableStore.YES : SerializableStore.NO;
	}

	public boolean isIndexed() {
		return field.isIndexed();
	}

	public float getBoost() {
		return field.getBoost();
	}

	public boolean getOmitNorms() {
		return field.getOmitNorms();
	}

	public boolean getOmitTermFreqAndPositions() {
		return field.getOmitTermFreqAndPositions();
	}
}
