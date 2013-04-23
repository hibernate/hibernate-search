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
package org.hibernate.search.analyzer;

/**
 * Returns the expected discriminator name to use on the element evaluated
 *
 * @author Hardy Ferentschik
 */
public interface Discriminator {

	/**
	 * Allows to specify the analyzer to be used for the given field based on the specified entity state.
	 *
	 * @param value The value of the field the <code>@AnalyzerDiscriminator</code> annotation was placed on. <code>null</code>
	 * if the annotation was placed on class level.
	 * @param entity The entity to be indexed.
	 * @param field The document field.
	 * @return The name of a defined analyzer to be used for the specified <code>field</code> or <code>null</code> if the
	 * default analyzer for this field should be used.
	 * @see org.hibernate.search.annotations.AnalyzerDef
	 */
	String getAnalyzerDefinitionName(Object value, Object entity, String field);
}
