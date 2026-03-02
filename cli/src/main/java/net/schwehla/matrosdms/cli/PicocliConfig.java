/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * Makes Spring the factory used by picocli to instantiate sub-commands.
 * When a sub-command class is a Spring bean (e.g. {@code @Component}) it is
 * looked up from the application context so that {@code @Autowired} fields
 * are injected normally.  For plain classes picocli's default factory is used
 * as fallback.
 */
@Configuration
public class PicocliConfig {

    @Autowired
    private ApplicationContext ctx;

    @Bean
    public IFactory picocliIFactory() {
        return new IFactory() {
            @Override
            public <K> K create(Class<K> type) throws Exception {
                try {
                    return ctx.getBean(type);
                } catch (Exception e) {
                    return CommandLine.defaultFactory().create(type);
                }
            }
        };
    }
}
