/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl.spi;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;

public interface IndexCompositeNodeBuilder extends IndexSchemaBuildContext {

	/**
	 * Create a new field and add it to the current builder.
	 *
	 * @param <F> The type of values for the new field
	 * @param relativeFieldName The relative name of the new field
	 * @param inclusion Whether fields matching this template should be included, provided their parent is included.
	 * @param indexFieldType The type of the new field
	 * @return A DSL step where the field can be defined in more details.
	 */
	<F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> addField(String relativeFieldName,
			IndexFieldInclusion inclusion, IndexFieldType<F> indexFieldType);

	/**
	 * Create a new named predicate and add it to the current builder.
	 *
	 * @param relativeNamedPredicateName The relative name of the new named predicate.
	 * @param inclusion Whether fields matching this template should be included, provided their parent is included.
	 * @param definition The definition of the named predicate.
	 * @return A DSL step where the named predicate can be defined in more details.
	 */
	IndexSchemaNamedPredicateOptionsStep addNamedPredicate(String relativeNamedPredicateName,
			IndexFieldInclusion inclusion, PredicateDefinition definition);

	/**
	 * Create a new object field and add it to the current builder.
	 *
	 * @param relativeFieldName The relative name of the new object field
	 * @param inclusion Whether fields matching this template should be included, provided their parent is included.
	 * @param structure The structure of the new object field
	 * @return A builder for the new object field
	 */
	IndexObjectFieldBuilder addObjectField(String relativeFieldName, IndexFieldInclusion inclusion,
			ObjectStructure structure);

	/**
	 * Create a new field template and add it to the current builder.
	 *
	 * @param templateName The name of the new template
	 * @param inclusion Whether fields matching this template should be included, provided their parent is included.
	 * @param indexFieldType The type of the new field template
	 * @param prefix A prefix to prepend to the {@link IndexSchemaFieldTemplateOptionsStep#matchingPathGlob(String) glob pattern}
	 * and to field paths passed to {@link org.hibernate.search.engine.backend.document.DocumentElement#addValue(String, Object)}.
	 * @return A DSL step where the field template can be defined in more details.
	 */
	IndexSchemaFieldTemplateOptionsStep<?> addFieldTemplate(String templateName, IndexFieldInclusion inclusion,
			IndexFieldType<?> indexFieldType,
			String prefix);

	/**
	 * Create a new object field template and add it to the current builder.
	 *
	 * @param templateName The name of the new template
	 * @param structure The structure of the new object field template
	 * @param prefix A prefix to prepend to the {@link IndexSchemaFieldTemplateOptionsStep#matchingPathGlob(String) glob pattern}
	 * and to field paths passed to {@link org.hibernate.search.engine.backend.document.DocumentElement#addObject(String)}.
	 * @param inclusion Whether fields matching this template should be included, provided their parent is included.
	 * @return A DSL step where the field template can be defined in more details.
	 */
	IndexSchemaFieldTemplateOptionsStep<?> addObjectFieldTemplate(String templateName, ObjectStructure structure,
			String prefix, IndexFieldInclusion inclusion);

}
