package com.example.frameworkplusplus;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ConfigurationApplication {

    public static void main1(String[] args) throws Exception {

        var applicationContext = new AnnotationConfigApplicationContext(CustomerJavaConfiguration.class);
        applicationContext.start();
        var runner = applicationContext.getBean(CustomerRepositoryRunner.class);
        runner.run(args);
        Thread.sleep(1000);

        applicationContext.close();

    }

}


// 0. ingest from (Java Config, Component Scanning, BEanRegistrars, etc.)
// 1. BeanDefinition (inspect with BeanFactoryPostProcessor)
// 2. Beans

class TxBeanPostProcessor implements BeanPostProcessor {

    private final ObjectProvider<TransactionTemplate> transactionTemplate;

    TxBeanPostProcessor(ObjectProvider<TransactionTemplate> transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof Tx tx) {
            IO.println("creating transactional proxy for " + beanName);
            return Transactions.transactional(this.transactionTemplate.getIfAvailable(), tx);
        }

        return bean;
    }
}

class MyBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor,
        BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
//        registry.registerBeanDefinition("foo", BeanDefinitionBuilder.rootBeanDefinition(...));
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        for (var beanName : beanFactory.getBeanDefinitionNames()) {
            var beanDefinition = beanFactory.getBeanDefinition(beanName);
            var type = beanFactory.getType(beanName);

            IO.println("beanName: " + beanName + ", description: " + beanDefinition
                    .getDescription() + ", type: " + type);


        }

    }
}

@Description("my other bean")
@Component
class MyThing {

}

@EnableTransactionManagement
@PropertySource("application.properties")
@ComponentScan
@Configuration
class CustomerJavaConfiguration {

    @Bean
    TxBeanPostProcessor txBeanPostProcessor(ObjectProvider<TransactionTemplate> transactionTemplate) {
        return new TxBeanPostProcessor(transactionTemplate);
    }

    @Bean
    static MyBeanFactoryPostProcessor myBeanFactoryPostProcessor() {
        return new MyBeanFactoryPostProcessor();
    }

    @Bean
    @Description("my special custom bean thingy ")
    CustomerRepositoryRunner customerRepositoryRunner(
            CustomerRepository repository) {
        return new CustomerRepositoryRunner(repository);
    }

    @Bean(name = "db")
    DriverManagerDataSource driverManagerDataSource(@Value("${spring.datasource.username}") String username,
                                                    Environment environment) {
        var pw = environment.getProperty("spring.datasource.password");
        var url = environment.getProperty("spring.datasource.url");
        return new DriverManagerDataSource(url, username, pw);
    }

    @Bean
    JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager platformTransactionManager) {
        return new TransactionTemplate(platformTransactionManager);
    }

    @Bean
    JdbcCustomerRepository customerRepository(JdbcClient jdbcClient) {
        return new JdbcCustomerRepository(jdbcClient);
    }

}

class CustomerRepositoryRunner {

    private final CustomerRepository repository;

    CustomerRepositoryRunner(@Qualifier("foo") CustomerRepository repository) {
        this.repository = repository;
    }

    void run(String[] args) throws Exception {
        repository.findAll().forEach(IO::println);
        var saved = repository.saveAll(List.of(new Customer(null, "Alexey"), new Customer(null, "Josh")));
        saved.forEach(IO::println);
    }

}

interface Tx {
}

abstract class Transactions {

    static <T> T transactional(TransactionTemplate transactionTemplate, T target) {
        return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), target.getClass().getInterfaces(),
                (_, method, args) -> {
                    return transactionTemplate.execute(_ -> {
                        try {
                            IO.println("calling " + method.getName() + " with arguments " + Arrays.toString(args));
                            return method.invoke(target, args);
                        } //
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

    }

}

interface CustomerRepository {

    Collection<Customer> findAll();

    Collection<Customer> findById(Iterable<Integer> ids);

    Collection<Customer> saveAll(Collection<Customer> customers);

}



@Transactional
class JdbcCustomerRepository implements CustomerRepository {

    private final RowMapper<Customer> rowMapper = (rs, _) -> new Customer(rs.getInt("id"), rs.getString("name"));

    private final JdbcClient db;

    JdbcCustomerRepository(JdbcClient jdbcClient) {
        this.db = jdbcClient;
    }

    @Override
    public Collection<Customer> findAll() {
        return this.db.sql("select * from customer").query(this.rowMapper).list();
    }

    @Override
    public Collection<Customer> findById(Iterable<Integer> ids) {
        return this.db.sql("select * from customer where id = any(?)")
                .params(new SqlArrayValue("int4", (Object[]) from(ids)))
                .query(this.rowMapper)
                .list();
    }

    @Override
    public Collection<Customer> saveAll(Collection<Customer> customers) {
        var ids = new ArrayList<Integer>();
        var counter = 0;
        for (var c : customers) {
            var keyHolder = new GeneratedKeyHolder();
            // if (counter == 1) throw new IllegalStateException("ooops!");
            this.db.sql("insert into customer ( name) values (?)").params(c.name()).update(keyHolder);
            var id = (Number) keyHolder.getKeyList().getFirst().get("id");
            ids.add(id.intValue());
            counter++;
        }
        return findById(ids);
    }

    private <T> T[] from(Iterable<T> iterable) {
        var list = new ArrayList<T>();
        iterable.forEach(list::add);
        return list.toArray((T[]) Array.newInstance(Object.class, list.size()));
    }

}

record Customer(Integer id, String name) {
}