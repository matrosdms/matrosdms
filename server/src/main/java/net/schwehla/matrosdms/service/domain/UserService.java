/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import net.schwehla.matrosdms.domain.core.MUser;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.repository.UserRepository;
import net.schwehla.matrosdms.service.mapper.MUserMapper;
import net.schwehla.matrosdms.service.message.CreateUserMessage;
import net.schwehla.matrosdms.service.message.SavedSearchMessage;
import net.schwehla.matrosdms.service.message.UpdateUserMessage;
import net.schwehla.matrosdms.util.UUIDProvider;

@Service
@Transactional
public class UserService {

	@Autowired
	UserRepository userRepository;
	@Autowired
	MUserMapper userMapper;
	@Autowired
	UUIDProvider uuidProvider;
	@Autowired
	PasswordEncoder passwordEncoder;

	@Transactional(readOnly = true)
	public long getUserCount() {
		return userRepository.count();
	}

	@Transactional(readOnly = true)
	public MUser login(String username, String rawPassword) {
		DBUser user = userRepository
				.findByName(username)
				.orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			throw new BadCredentialsException("Invalid username or password");
		}
		return userMapper.entityToModel(user);
	}

	@Caching(evict = { @CacheEvict(value = "userList", allEntries = true) })
	public MUser createUser(CreateUserMessage user) {
		DBUser dbUser = userMapper.modelToEntity(user);
		dbUser.setUuid(uuidProvider.getTimeBasedUUID());
		dbUser.setPassword(passwordEncoder.encode(user.getPassword()));
		DBUser savedUser = userRepository.save(dbUser);
		return userMapper.entityToModel(savedUser);
	}

	@Caching(evict = {
			@CacheEvict(value = "userList", allEntries = true),
			@CacheEvict(value = "users", key = "#uuid")
	})
	public MUser updateUser(String uuid, UpdateUserMessage msg) {
		DBUser dbUser = userRepository
				.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		userMapper.updateEntity(msg, dbUser);
		if (msg.getPassword() != null && !msg.getPassword().isBlank()) {
			dbUser.setPassword(passwordEncoder.encode(msg.getPassword()));
		}
		return userMapper.entityToModel(userRepository.save(dbUser));
	}

	@Cacheable(value = "userList")
	public List<MUser> loadUserList() {
		return userMapper.map(userRepository.findAll());
	}

	@Cacheable(value = "users", key = "#uuid")
	public MUser loadUserDetail(String uuid) {
		var user = userRepository
				.findByUuid(uuid)
				.orElseThrow(
						() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + uuid));
		return userMapper.entityToModel(user);
	}

	@Caching(evict = {
			@CacheEvict(value = "userList", allEntries = true),
			@CacheEvict(value = "users", key = "#uuid")
	})
	public void deleteUser(String uuid) {
		userRepository.delete(
				userRepository
						.findByUuid(uuid)
						.orElseThrow(
								() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + uuid)));
	}

	@Caching(evict = {
			@CacheEvict(value = "userList", allEntries = true),
			@CacheEvict(value = "users", allEntries = true)
	})
	public void deleteAllUsers() {
		userRepository.deleteAll();
	}

	// --- SAVED SEARCH LOGIC ---

	@Transactional(readOnly = true)
	public List<SavedSearchMessage> getSavedSearches(String userUuid) {
		DBUser user = userRepository
				.findByUuid(userUuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		// Return a copy to avoid Hibernate modification issues if read-only
		return List.copyOf(user.getSavedSearches());
	}

	@Transactional
	public void addSavedSearch(String userUuid, SavedSearchMessage search) {
		DBUser user = userRepository
				.findByUuid(userUuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		// removeIf handles duplicates (Update by Name)
		user.getSavedSearches().removeIf(s -> s.getName().equalsIgnoreCase(search.getName()));
		user.getSavedSearches().add(search);

		// Explicit save ensures the Converter is triggered and JSON is written
		userRepository.save(user);
	}

	@Transactional
	public void removeSavedSearch(String userUuid, String name) {
		DBUser user = userRepository
				.findByUuid(userUuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		boolean removed = user.getSavedSearches().removeIf(s -> s.getName().equalsIgnoreCase(name));
		if (removed) {
			userRepository.save(user);
		}
	}
}
