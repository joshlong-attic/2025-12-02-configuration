package com.example.boot;


import a.b.c.GreetingApplicationRunner;
import a.b.c.NameProducer;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


@Configuration
@EnableResilientMethods
class ResilientMethods {
}

@Component
class MyFlakyClient {

    @ConcurrencyLimit(10)
    @Retryable(maxRetries = 10)
    String callService() {
        return null; //todo
    }
}

@Component
class Cart implements Serializable {
}

@SpringBootApplication
@ImportRuntimeHints(ConfigurationApplication.Hints.class)
public class ConfigurationApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(ConfigurationApplication.class, args);
    }

    @Bean
    static SerializationBeanFactoryInitializationAotProcessor serializationBeanFactoryInitializationAotProcessor() {
        return new SerializationBeanFactoryInitializationAotProcessor();
    }

    static class SerializationBeanFactoryInitializationAotProcessor implements
            BeanFactoryInitializationAotProcessor {

        @Override
        public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(
                ConfigurableListableBeanFactory beanFactory) {

            var serializable = new HashSet<TypeReference>();

            for (var name : beanFactory.getBeanDefinitionNames()) {
                var type = beanFactory.getType(name);
                if (Serializable.class.isAssignableFrom(type)) {
                    serializable.add(TypeReference.of(type));
                }
            }
            IO.println(serializable);

            return (generationContext, _) -> serializable.forEach(tr ->
                    generationContext.getRuntimeHints().serialization().registerType(tr));
        }
    }

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(@Nullable RuntimeHints hints, @Nullable ClassLoader classLoader) {
            hints.resources().registerResource(FILE);
//            hints.reflection().registerType()

        }
    }

    static final Resource FILE = new ClassPathResource("/message");

    @Bean
    ApplicationRunner breakGraalVmNativeImage() {
        return args -> {
            var contents = FILE.getContentAsString(Charset.defaultCharset());
            IO.println("contents: " + contents);
        };
    }

    @Bean
    NameProducer producer() {
        return () -> "Spring fans";
    }

    @Bean
    GreetingApplicationRunner runner() {
        return new GreetingApplicationRunner("Bob");
    }
}

@Component
class CustomerRepositoryRunner implements CommandLineRunner {

    private final CustomerRepository repository;

    CustomerRepositoryRunner(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        repository.findAll().forEach(IO::println);
        var saved = repository.saveAll(List.of(new Customer(null, "Alexey"), new Customer(null, "Josh")));
        saved.forEach(IO::println);
    }

}

interface CustomerRepository {

    Collection<Customer> findAll();

    Collection<Customer> findById(Iterable<Integer> ids);

    Collection<Customer> saveAll(Collection<Customer> customers);

}

@Repository
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