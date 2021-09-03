/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.junit.runner.RunWith;

/**
 * Same test as {@link AutomaticIndexingAssociationDeletionIT},
 * but with bytecode enhancement enabled, so that lazy one-to-one associations actually work.
 */
@TestForIssue(jiraKey = { "HSEARCH-3999", "HSEARCH-4303" })
@RunWith(BytecodeEnhancerRunner.class) // So that we can have lazy *ToOne associations
@EnhancementOptions(lazyLoading = true)
public class AutomaticIndexingAssociationDeletionBytecodeEnhancementIT
		extends AutomaticIndexingAssociationDeletionIT {

	@Override
	protected OrmSetupHelper.SetupContext configure(OrmSetupHelper.SetupContext ctx) {
		return ctx
				// Necessary for BytecodeEnhancerRunner, see BytecodeEnhancementIT.setup
				.withTcclLookupPrecedenceBefore()
				// So that we can have lazy *ToOne associations
				.withProperty( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
	}
}
