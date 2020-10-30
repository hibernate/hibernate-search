/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.reindexonupdate.shallow.incorrect;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;

import org.junit.Rule;
import org.junit.Test;

public class ReindexOnUpdateShallowIncorrectIT {

	@Rule
	public DocumentationSetupHelper setupHelper =
			DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@Test
	public void missingReindexOnUpdateShallow() {
		assertThatThrownBy( () -> setupHelper.start().setup( Book.class, BookCategory.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( Book.class.getName() )
						.pathContext( ".category<no value extractors>.name<no value extractors>" )
						.failure(
								"Unable to find the inverse side of the association on type '"
										+ Book.class.getName() + "' at path '.category<no value extractors>'",
								"Hibernate Search needs this information",
								"you can disable automatic reindexing with"
										+ " @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)"
						)
						.build() );
	}

}
