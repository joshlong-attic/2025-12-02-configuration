package com.example.framework;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.annotation.*;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// dependency injection
// portable service abstractions
// AOP (aspect-oriented programming)
// autoconfiguration

// spring ai, spring modulith, spring cloud, etc.
// spring boot
// spring data, spring security, integration, batch, shell, etc.
// spring framework

public class ConfigurationApplication {

	public static void main0(String[] args) throws Exception {

		var applicationContext = new AnnotationConfigApplicationContext(CustomerJavaConfiguration.class);
		applicationContext.start();
		var runner = applicationContext.getBean(CustomerRepositoryRunner.class);
		runner.run(args);
		Thread.sleep(1000);

		applicationContext.close();

		// eval(" 1 + 1") -> 2
		// CP30, Apache DBCP, Apache Tomcat Pool, HikariCP
	}

}

// ruby on rails
// david heinemeier hansson
// 5 minute demo
// "convention over configuration"
//

// java configuration - explicit
// component scanning - implicit
// BeanRegistrar (new in Spring Framework 7)

class CustomerBeanRegistrar implements BeanRegistrar {

	@Override
	public void register(@NonNull BeanRegistry registry, @NonNull Environment env) {

		/*
		 * registry.registerBean(CustomerRepositoryRunner.class, spec -> spec
		 * .supplier(supplierContext -> { var repository =
		 * supplierContext.bean(CustomerRepository.class) ; return new
		 * CustomerRepositoryRunner(repository ); }));
		 */

	}

}

@Component
class MyLifecycle implements InitializingBean, DisposableBean {

	private final DataSource dataSource;

	private @Nullable PlatformTransactionManager transactionManager;

	MyLifecycle(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Autowired
	public void setTransactionManager(@Nullable PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@PostConstruct
	void start() {
		if (this.transactionManager == null) {
			IO.println("no default transaction manager ...");
		}
		IO.println("start");
	}

	@PreDestroy
	void stop() {
		IO.println("stop");
	}

	@Override
	public void destroy() throws Exception {
		IO.println("destroy in interface");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		IO.println("afterPropertiesSet in interface");
	}

}

class Foo {

	Foo() {
		IO.println("Foo");
	}

}

class Bar {

	private final Foo foo;

	Bar(Foo foo) {
		this.foo = foo;
	}

}

@Configuration
class FooBarConfiguration {

	// @Scope ("prototype")
	// @Scope ("singleton")
	@Bean
	Foo foo() {
		return new Foo();
	}

	@Bean
	Bar bar(/* Foo foo */) {

		var foo = this.foo();

		for (var i = 0; i < 10; i++) {
			this.foo();
		}

		return new Bar(foo);
	}

}

@PropertySource("application.properties")
@ComponentScan
@Configuration
@Import({ CustomerBeanRegistrar.class })
class CustomerJavaConfiguration {

	// @Bean
	// ApplicationRunner runner (Map <String, DataSource> dataSourceMap) {
	// return args -> IO.println(dataSourceMap );
	// }
	//
	@Bean
	CustomerRepositoryRunner fooRunner(
			/*
			 * ObjectProvider<CustomerRepository> objectProvider,
			 *
			 * @Qualifier("another") CustomerRepository a, Map<String, CustomerRepository>
			 * map, CustomerRepository[] arrays
			 */
			CustomerRepository repository) {
		/*
		 * IO.println("====="); objectProvider.forEach( r -> IO.println("found  " +r ));
		 * IO.println("=====");
		 *
		 * IO.println(map); for (var arr : arrays) IO.println("\t" + arr);
		 * IO.println(map.get("another") == a); return new
		 * CustomerRepositoryRunner(map.values().iterator().next());
		 */
		return new CustomerRepositoryRunner(repository);
	}

	@Bean
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
	CustomerRepository customerRepository(JdbcClient jdbcClient, TransactionTemplate transactionTemplate) {
		var raw = new JdbcCustomerRepository(jdbcClient);
		return Transactions.transactional(transactionTemplate, raw);
	}

}

@Component
class EventConsumer {

	@EventListener
	void onAuthentication(UserAuthenticatedEvent userAuthenticatedEvent) {
		IO.println("onAuthentication " + userAuthenticatedEvent);
	}

}

@Component
class EventProducer /* implements ApplicationEventPublisherAware */ {

	private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	EventProducer(ApplicationEventPublisher publisher) {
		this.service.schedule(() -> {
			var jlong = new UserAuthenticatedEvent(Instant.now(), "jlong");
			publisher.publishEvent(jlong);
		}, 1L, TimeUnit.SECONDS);

	}

}

record UserAuthenticatedEvent(Instant when, String username) {
}

// UML (unified modeling language)
// UML stereotypes

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@interface AlexeyComponent {

	/**
	 * Alias for {@link Component#value}.
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

}

@AlexeyComponent
class MyService {

	MyService() {
		IO.println("MyService constructor");
	}

}

/*
 *
 * @Repository("another") class AnotherCustomerRepository implements CustomerRepository {
 *
 * @Override public Collection<Customer> findAll() { return List.of(); }
 *
 * @Override public Collection<Customer> findById(Iterable<Integer> ids) { return
 * List.of(); }
 *
 * @Override public Collection<Customer> saveAll(Collection<Customer> customers) { return
 * List.of(); } }
 *
 * @Repository class MyOtherCustomerRepository implements CustomerRepository {
 *
 * @Override public Collection<Customer> findAll() { return List.of(); }
 *
 * @Override public Collection<Customer> findById(Iterable<Integer> ids) { return
 * List.of(); }
 *
 * @Override public Collection<Customer> saveAll(Collection<Customer> customers) { return
 * List.of(); } }
 */
@Qualifier("apple")
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@interface Apple {

}

@Qualifier("google")
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@interface Google {

}

@Component
class AppStoreConsumer {

	AppStoreConsumer(@Apple AppStore apple, @Google AppStore google) {
		IO.println("AppStoreConsumer " + apple + " " + google);
	}

}

interface AppStore {

}

@Component
@Apple
class AppleAppStore implements AppStore {

}

@Component
@Google
class GoogleAppStore implements AppStore {

}

// meta-annotation
// @Component
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

// jdbc, jpa, jdo, jms, kafka, rabbitmq, neo4j, mongodb, google cloud spanner, cassandra,
// etc. etc. etc.
class TransactionalCustomerRepository implements CustomerRepository {

	private final CustomerRepository repository;

	private final TransactionTemplate transactionTemplate;

	TransactionalCustomerRepository(TransactionTemplate transactionTemplate, CustomerRepository repository) {
		this.repository = repository;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public Collection<Customer> findAll() {
		return this.transactionTemplate.execute(_ -> this.repository.findAll());
	}

	@Override
	public Collection<Customer> findById(Iterable<Integer> ids) {
		return this.transactionTemplate.execute(_ -> this.repository.findById(ids));
	}

	@Override
	public Collection<Customer> saveAll(Collection<Customer> customers) {
		return this.transactionTemplate.execute(_ -> repository.saveAll(customers));
	}

}

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