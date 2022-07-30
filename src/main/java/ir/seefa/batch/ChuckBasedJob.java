package ir.seefa.batch;

import ir.seefa.mapper.CustomerRowMapper;
import ir.seefa.model.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Saman Delfani
 * @version 1.0
 * @since 2022-07-30 05:54:22
 */
@Configuration
public class ChuckBasedJob {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    public static String customer_query_sql = "SELECT customerNumber, customerName, contactLastName, contactFirstName, phone, addressLine1, addressLine2, city, state, postalCode, country, salesRepEmployeeNumber, creditLimit FROM `spring-batch`.customers ORDER BY customerNumber";
    private final AtomicInteger numberOfCustomers = new AtomicInteger();
    private final DataSource dataSource;

    public ChuckBasedJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, DataSource dataSource) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.dataSource = dataSource;
    }

    @Bean
    public ItemReader<Customer> itemReader() {
        return new JdbcCursorItemReaderBuilder<Customer>()
                .dataSource(dataSource)
                .name("jdbcCursorItemReader")
                .sql(customer_query_sql)
                .rowMapper(new CustomerRowMapper())
                .build();
    }

    @Bean
    public Step chunkBasedReadingFlatFileStep() {
        return this.stepBuilderFactory.get("chunkBasedReadingFromDBInMultiThreadScenarios")
                .<Customer, Customer>chunk(10)
                .reader(itemReader())
                .writer(items -> {
                    numberOfCustomers.getAndAdd(items.size());
                    items.forEach(System.out::println);
                    System.out.println("Number of Customers: " + numberOfCustomers.get());
                })
                .build();
    }

    @Bean
    public Job chuckOrientedJob() {
        return this.jobBuilderFactory.get("chunkOrientedReadingDbInSingleThreadScenariosJob")
                .start(chunkBasedReadingFlatFileStep())
                .build();

    }
}
