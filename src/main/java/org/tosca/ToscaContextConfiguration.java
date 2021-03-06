package org.tosca;

import org.springframework.context.annotation.*;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import alien4cloud.tosca.parser.ToscaParser;
import alien4cloud.tosca.repository.LocalRepositoryImpl;

/**
 * Context configuration for TOSCA Parser.
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan({ "alien4cloud.tosca", "alien4cloud.utils.services", "alien4cloud.paas.wf" })
public class ToscaContextConfiguration {
    @Bean(name = "validator")
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    private static AnnotationConfigApplicationContext applicationContext = null;

    /**
     * If you are not using SpringFramework in your application, this utility method will load an application and provide you with an instance of the
     * ToscaParser.
     * 
     * @return The instance of the ToscaParser
     */
    public static ToscaParser getParser() {
        return getParser(null);
    }

    /**
     * Get the parser and set the path of the archive local repository
     * 
     * @param localRepositoryPath The path of the local repository if not already initialized.
     * @return The path of the local repository.
     */
    public static ToscaParser getParser(String localRepositoryPath) {
        if (applicationContext == null) {
            initContext();
            applicationContext.getBean(LocalRepositoryImpl.class).setPath(localRepositoryPath);
        }
        return applicationContext.getBean(ToscaParser.class);
    }

    private static synchronized void initContext() {
        applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(ToscaContextConfiguration.class);
        applicationContext.refresh();
        applicationContext.start();
    }
}
