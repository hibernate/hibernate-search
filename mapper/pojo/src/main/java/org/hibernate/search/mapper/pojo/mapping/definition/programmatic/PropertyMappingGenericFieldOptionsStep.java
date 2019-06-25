/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

/**
 * The step in a property-to-index-field mapping where optional parameters can be set,
 * when the index field is a generic field.
 */
public interface PropertyMappingGenericFieldOptionsStep
		extends PropertyMappingNonFullTextFieldOptionsStep<PropertyMappingGenericFieldOptionsStep> {

}
