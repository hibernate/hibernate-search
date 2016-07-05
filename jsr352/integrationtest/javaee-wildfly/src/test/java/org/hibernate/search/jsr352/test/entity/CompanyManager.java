package org.hibernate.search.jsr352.test.entity;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.search.Query;
import org.hibernate.CacheMode;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.jboss.ejb3.annotation.TransactionTimeout;

@Stateless
public class CompanyManager {

    @PersistenceContext(name="h2")
    private EntityManager em;
    
    @TransactionTimeout(value=5, unit=TimeUnit.MINUTES)
    public void persist(Iterable<Company> companies) {
        for (Company company: companies) {
            em.persist(company);
        }
    }
    
    public List<Company> findCompanyByName(String name) {
        FullTextEntityManager ftem = Search.getFullTextEntityManager(em);
        Query luceneQuery = ftem.getSearchFactory().buildQueryBuilder()
                .forEntity(Company.class).get()
                    .keyword().onField("name").matching(name)
                .createQuery();
        @SuppressWarnings("unchecked")
        List<Company> result = ftem.createFullTextQuery(luceneQuery).getResultList();
        return result;
    }
    
    public void indexCompany() {
//      Set<Class<?>> rootEntities = new HashSet<>();
//      rootEntities.add(Company.class);
//      // org.hibernate.search.jsr352.MassIndexer
//      MassIndexer massIndexer = new MassIndexerImpl().rootEntities(rootEntities);
//      long executionId = massIndexer.start();
//      logger.infof("job execution id = %d", executionId);
        try {
            Search.getFullTextEntityManager( em )
                .createIndexer()
                .batchSizeToLoadObjects( 1 )
                .threadsToLoadObjects( 1 )
                .transactionTimeout( 10 )
                .cacheMode( CacheMode.IGNORE )
                .startAndWait();
        }
        catch (InterruptedException e) {
            throw new RuntimeException( e );
        }
    }
    
    public EntityManager getEntityManager() {
        return em;
    }
}
