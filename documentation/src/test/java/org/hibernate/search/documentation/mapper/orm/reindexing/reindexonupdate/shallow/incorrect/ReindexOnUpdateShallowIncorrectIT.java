/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.reindexonupdate.shallow.incorrect;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ReindexOnUpdateShallowIncorrectIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper =
			DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@Test
	void missingReindexOnUpdateShallow() {
		assertThatThrownBy( () -> setupHelper.start().setup( Book.class, BookCategory.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Book.class.getName() )
						.pathContext( ".category<no value extractors>.name<no value extractors>" )
						.failure(
								"Unable to find the inverse side of the association on type '"
										+ Book.class.getName() + "' at path '.category<no value extractors>'",
								"Hibernate Search needs this information",
								"you can disable automatic reindexing with"
										+ " @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)"
						) );
	}

}
