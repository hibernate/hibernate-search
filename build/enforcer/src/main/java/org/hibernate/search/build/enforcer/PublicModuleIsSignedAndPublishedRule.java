/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.build.enforcer;

import static org.hibernate.search.build.enforcer.MavenProjectUtils.isAnyParentPublicParent;
import static org.hibernate.search.build.enforcer.MavenProjectUtils.isProjectNotDeployed;
import static org.hibernate.search.build.enforcer.MavenProjectUtils.isProjectSigned;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

@Named("publicModuleIsSignedAndPublishedRule") // rule name - must start with lowercase character
public class PublicModuleIsSignedAndPublishedRule extends AbstractEnforcerRule {


	// Inject needed Maven components
	@Inject
	private MavenSession session;

	public void execute() throws EnforcerRuleException {
		MavenProject currentProject = session.getCurrentProject();
		boolean projectSigned = isProjectSigned( currentProject );
		boolean projectNotDeployed = isProjectNotDeployed( currentProject );
		if ( isAnyParentPublicParent( currentProject ) && ( projectNotDeployed || !projectSigned ) ) {
			throw new EnforcerRuleException(
					"Project " + currentProject.getArtifactId() + " is considered public but is *not*: [ "
							+ ( projectSigned ? "" : "signed (consider setting release.gpg.signing.skip to false)" )
							+ ( projectNotDeployed ? "deployed (consider setting skipNexusStagingDeployMojo to false)" : "" )
							+ "]"
			);
		}
	}


}
