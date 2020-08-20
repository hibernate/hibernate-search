/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
