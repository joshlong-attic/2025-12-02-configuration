package a.b.c;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

//@ConditionalOnClass ()
@Configuration
class AlexseyAutoConfiguration {

    // ruby on rails
    // code generated

    // FRAMEWORK

    // open-closed principle
    // "open for extension, but closed for modification"

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(NameProducer.class)
    GreetingApplicationRunner nameProducerRunner(NameProducer producer) {
        return new GreetingApplicationRunner(
                producer.name()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("alexsey.message.name")
    GreetingApplicationRunner propertyRunner(Environment environment) {
        var property = environment.getProperty("alexsey.message.name");
        return new GreetingApplicationRunner(property);
    }

    @Bean
    @Conditional(AlexseyIsProductiveCondition.class)
    ApplicationRunner isProductiveRunner() {
        return a -> IO.println("Alexsey is productive!");
    }
}

class AlexseyIsProductiveCondition extends SpringBootCondition {


    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return ConditionOutcome.match();
    }
}