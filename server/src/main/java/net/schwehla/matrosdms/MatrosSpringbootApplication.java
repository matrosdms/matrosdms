/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.schwehla.matrosdms.util.UUIDProvider;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@EnableAsync
@EnableCaching
@EntityScan("net.schwehla.matrosdms.entity")
@EnableJpaRepositories("net.schwehla.matrosdms.repository")
public class MatrosSpringbootApplication {

	Logger logger = LoggerFactory.getLogger(MatrosSpringbootApplication.class);

	UUIDProvider uuidProvider = new UUIDProvider();

	@Bean
	UUIDProvider uuidProvider() {
		return uuidProvider;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// https://www.baeldung.com/spring-security-registration-password-encoding-bcrypt
		return new BCryptPasswordEncoder();
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(MatrosSpringbootApplication.class, args);
	}
}
