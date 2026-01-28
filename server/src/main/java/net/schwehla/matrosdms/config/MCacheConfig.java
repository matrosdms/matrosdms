/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class MCacheConfig {

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager manager = new CaffeineCacheManager();

		// 1. Define the BUILDER (Do not call .build() yet!)
		Caffeine<Object, Object> singleEntityBuilder = Caffeine.newBuilder().maximumSize(2000).expireAfterAccess(30,
				TimeUnit.MINUTES);

		// 2. Build NEW instances for each cache
		manager.registerCustomCache("items", singleEntityBuilder.build());
		manager.registerCustomCache("contexts", singleEntityBuilder.build());
		manager.registerCustomCache("users", singleEntityBuilder.build());
		manager.registerCustomCache("stores", singleEntityBuilder.build());
		manager.registerCustomCache("actions", singleEntityBuilder.build());

		// ---------------------------------------------------------

		// 3. Define the List BUILDER (Do not call .build() yet!)
		Caffeine<Object, Object> listBuilder = Caffeine.newBuilder().maximumSize(500).expireAfterWrite(5,
				TimeUnit.MINUTES);

		// 4. Build NEW instances for each list cache
		manager.registerCustomCache("itemList", listBuilder.build());
		manager.registerCustomCache("contextList", listBuilder.build());
		manager.registerCustomCache("userList", listBuilder.build());
		manager.registerCustomCache("storeList", listBuilder.build());
		manager.registerCustomCache("actionList", listBuilder.build());

		// ---------------------------------------------------------

		manager.registerCustomCache(
				"categories",
				Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(2, TimeUnit.HOURS).build());

		manager.setCaffeine(
				Caffeine.newBuilder().maximumSize(100).expireAfterWrite(10, TimeUnit.MINUTES));

		return manager;
	}
}
