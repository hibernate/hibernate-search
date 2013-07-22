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

package org.hibernate.search.test.metadata;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.test.util.FooAnalyzer;

/**
 * @author Hardy Ferentschik
 */
@Indexed
public class Snafu {
	@DocumentId
	private long id;

	@Field(name = "my-snafu",
			index = Index.NO,
			store = Store.YES,
			analyze = Analyze.NO,
			norms = Norms.NO,
			termVector = TermVector.WITH_POSITIONS,
			boost = @Boost(10.0f))
	private String snafu;

	@Field
	@NumericField(precisionStep = 16)
	private int numericField;

	@Field(indexNullAs = "snafu")
	private String nullValue;

	@Field
	@org.hibernate.search.annotations.Analyzer(impl = FooAnalyzer.class)
	private String custom;
}
