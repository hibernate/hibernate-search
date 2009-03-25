// $Id$
package org.hibernate.search.test.reader.performance;

import java.util.concurrent.CountDownLatch;

import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.reader.Detective;
import org.hibernate.search.test.reader.Suspect;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class InsertActivity extends AbstractActivity {
	
	InsertActivity(SessionFactory sf, CountDownLatch startSignal) {
		super(sf, startSignal);
	}

	@Override
	protected void doAction(FullTextSession s, int jobSeed) {
		Detective detective = new Detective();
		detective.setName("John Doe " + jobSeed);
		detective.setBadge("123455" + jobSeed);
		detective.setPhysicalDescription("Blond green eye etc etc");
		s.persist(detective);
		Suspect suspect = new Suspect();
		suspect.setName("Jane Doe " + jobSeed);
		suspect.setPhysicalDescription("brunette, short, 30-ish");
		if (jobSeed % 20 == 0) {
			suspect.setSuspectCharge("thief liar ");
		} else {
			suspect.setSuspectCharge(" It's 1875 in London. The police have captured career criminal Montmorency. In the process he has been grievously wounded and it is up to a young surgeon to treat his wounds. During his recovery Montmorency learns of the city's new sewer system and sees in it the perfect underground highway for his thievery.  Washington Post columnist John Kelly recommends this title for middle schoolers, especially to be read aloud.");
		}
		s.persist(suspect);
	}

}
