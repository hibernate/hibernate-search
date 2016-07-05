package org.hibernate.search.jsr352.se;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.MassIndexer;
import org.hibernate.search.jsr352.MassIndexerImpl;
import org.hibernate.search.jsr352.se.test.Company;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MassIndexerIT {

    private EntityManagerFactory emf;
    private EntityManager em;

    private JobOperator jobOperator;

    // mass indexer configuration values
    private final boolean OPTIMIZE_AFTER_PURGE = true;
    private final boolean OPTIMIZE_AT_END = true;
    private final boolean PURGE_AT_START = true;
    private final int ARRAY_CAPACITY = 500;
    private final int FETCH_SIZE = 100000;
    private final int MAX_RESULTS = 200 * 1000;
    private final int PARTITION_CAPACITY = 250;
    private final int PARTITIONS = 1;
    private final int THREADS = 1;

    // example dataset
    private final long DB_COMP_ROWS = 3;
    private final long DB_COMP_ROWS_LOADED = 3;
    private final Company COMPANY_1 = new Company("Google");
    private final Company COMPANY_2 = new Company("Red Hat");
    private final Company COMPANY_3 = new Company("Microsoft");

    private static final int JOB_MAX_TRIES = 240;       // 240 second
    private static final int JOB_THREAD_SLEEP = 1000;

    private static final Logger logger = Logger.getLogger(MassIndexerIT.class);

    @Before
    public void setup() {

        jobOperator = JobFactory.getJobOperator();
        emf = Persistence.createEntityManagerFactory("h2");
        em = emf.createEntityManager();

        em.getTransaction().begin();
        em.persist(COMPANY_1);
        em.persist(COMPANY_2);
        em.persist(COMPANY_3);
        em.getTransaction().commit();
    }

    @Test
    public void testMassIndexer() throws InterruptedException {

        logger.infof("finding company called %s ...", "google");
        List<Company> companies = findCompanyByName("google");
        assertEquals(0, companies.size());

        long executionId = indexCompany();
        JobExecution jobExecution = jobOperator.getJobExecution(executionId);
        jobExecution = keepTestAlive(jobExecution);
        List<StepExecution> stepExecutions = jobOperator.getStepExecutions(executionId);
        for (StepExecution stepExecution: stepExecutions) {
            logger.infof("step %s executed.", stepExecution.getStepName());
        }

        companies = findCompanyByName("google");
        assertEquals(1, companies.size());
    }

    private List<Company> findCompanyByName(String name) {
        FullTextEntityManager ftem = Search.getFullTextEntityManager(em);
        Query luceneQuery = ftem.getSearchFactory().buildQueryBuilder()
                .forEntity(Company.class).get()
                    .keyword().onField("name").matching(name)
                .createQuery();
        @SuppressWarnings("unchecked")
        List<Company> result = ftem.createFullTextQuery(luceneQuery).getResultList();
        return result;
    }

    private long indexCompany() throws InterruptedException {
        // org.hibernate.search.jsr352.MassIndexer
        MassIndexer massIndexer = new MassIndexerImpl()
                .addRootEntities(Company.class)
                .entityManager(em)
                .jobOperator(jobOperator);
        long executionId = massIndexer.start();

        logger.infof("job execution id = %d", executionId);
        return executionId;
//        try {
//            Search.getFullTextEntityManager( em )
//                .createIndexer()
//                .batchSizeToLoadObjects( 1 )
//                .threadsToLoadObjects( 1 )
//                .transactionTimeout( 10 )
//                .cacheMode( CacheMode.IGNORE )
//                .startAndWait();
//        }
//        catch (InterruptedException e) {
//            throw new RuntimeException( e );
//        }
    }

    public JobExecution keepTestAlive(JobExecution jobExecution) throws InterruptedException {
        int tries = 0;
        while (!jobExecution.getBatchStatus().equals(BatchStatus.COMPLETED)) {
            if (tries < JOB_MAX_TRIES) {
                tries++;
                Thread.sleep(JOB_THREAD_SLEEP);
                jobExecution = jobOperator.getJobExecution(jobExecution.getExecutionId());
            } else {
                break;
            }
        }
        return jobExecution;
    }

    private void testBatchStatus(StepExecution stepExecution) {
        BatchStatus batchStatus = stepExecution.getBatchStatus();
        switch (stepExecution.getStepName()) {

            case "loadId":
                long expectedEntityCount = DB_COMP_ROWS;
//              assertEquals(expectedEntityCount, indexingContext.getEntityCount());
                assertEquals(BatchStatus.COMPLETED, batchStatus);
                break;

            case "purgeDecision":
                assertEquals(BatchStatus.COMPLETED, batchStatus);
                break;

            case "purgeIndex":
                if (PURGE_AT_START) {
                    assertEquals(BatchStatus.COMPLETED, batchStatus);
                }
                break;

            case "afterPurgeDecision":
                assertEquals(BatchStatus.COMPLETED, batchStatus);
                break;

            case "optimizeAfterPurge":
                if (OPTIMIZE_AFTER_PURGE) {
                    assertEquals(BatchStatus.COMPLETED, batchStatus);
                }
                break;

            case "produceLuceneDoc":
                Metric[] metrics = stepExecution.getMetrics();
                testChunk(getMetricsMap(metrics));
                assertEquals(BatchStatus.COMPLETED, batchStatus);
                break;

            case "afterIndexDecision":
                assertEquals(BatchStatus.COMPLETED, batchStatus);
                break;

            case "optimizeAfterIndex":
                assertEquals(BatchStatus.COMPLETED, batchStatus);
                break;

            default:
                break;
        }
    }

    private void testChunk(Map<Metric.MetricType, Long> metricsMap) {
        long companyCount = (long) Math.ceil((double) DB_COMP_ROWS_LOADED / ARRAY_CAPACITY);
        // The read count.
        long expectedReadCount = companyCount;
        long actualReadCount = metricsMap.get(Metric.MetricType.READ_COUNT);
        assertEquals(expectedReadCount, actualReadCount);
        // The write count
        long expectedWriteCount = companyCount;
        long actualWriteCount = metricsMap.get(Metric.MetricType.WRITE_COUNT);
        assertEquals(expectedWriteCount, actualWriteCount);
    }

    /**
     * Convert the Metric array contained in StepExecution to a key-value map
     * for easy access to Metric parameters.
     *
     * @param metrics
     *         a Metric array contained in StepExecution.
     *
     * @return a map view of the metrics array.
     */
    public Map<Metric.MetricType, Long> getMetricsMap(Metric[] metrics) {
        Map<Metric.MetricType, Long> metricsMap = new HashMap<>();
        for (Metric metric : metrics) {
            metricsMap.put(metric.getType(), metric.getValue());
        }
        return metricsMap;
    }

    @After
    public void shutdownJPA() {
        em.close();
        emf.close();
    }
}
