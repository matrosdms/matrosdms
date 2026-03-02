/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import net.schwehla.matrosdms.cli.command.MatrosCommand;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication(
        scanBasePackages = "net.schwehla.matrosdms.cli",
        exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                FlywayAutoConfiguration.class
        })
public class CliApplication implements CommandLineRunner, ExitCodeGenerator {

    @Autowired
    private MatrosCommand matrosCommand;

    @Autowired
    private IFactory factory; // Spring-powered factory for Picocli DI

    private int exitCode;

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(CliApplication.class, args)));
    }

    @Override
    public void run(String... args) {
        exitCode = new CommandLine(matrosCommand, factory)
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args.length == 0 ? new String[]{"--help"} : args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}