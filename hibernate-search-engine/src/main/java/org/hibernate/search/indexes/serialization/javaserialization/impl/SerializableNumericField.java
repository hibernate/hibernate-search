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
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import org.hibernate.search.indexes.serialization.spi.LuceneNumericFieldContext;
import org.hibernate.search.indexes.serialization.spi.SerializableStore;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public abstract class SerializableNumericField implements SerializableFieldable {
	private String name;
	private int precisionStep;
	private SerializableStore store;
	private boolean indexed; //or should it be Index for future extension?
	private float boost;
	private boolean omitNorms;
	private boolean omitTermFreqAndPositions;

	public SerializableNumericField(LuceneNumericFieldContext context) {
		this.name = context.getName();
		this.precisionStep = context.getPrecisionStep();
		this.store = context.getStore();
		this.indexed = context.isIndexed();
		this.boost = context.getBoost();
		this.omitNorms = context.getOmitNorms();
		this.omitTermFreqAndPositions = context.getOmitTermFreqAndPositions();
	}

	public String getName() {
		return name;
	}

	public int getPrecisionStep() {
		return precisionStep;
	}

	public SerializableStore getStore() {
		return store;
	}

	public boolean isIndexed() {
		return indexed;
	}

	public float getBoost() {
		return boost;
	}

	public boolean isOmitNorms() {
		return omitNorms;
	}

	public boolean isOmitTermFreqAndPositions() {
		return omitTermFreqAndPositions;
	}
}
