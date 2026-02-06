/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity.management;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import net.schwehla.matrosdms.domain.core.EUserRole;
import net.schwehla.matrosdms.entity.AbstractDBInfoBaseEntity;
import net.schwehla.matrosdms.entity.converter.JpaJsonConverter;
import net.schwehla.matrosdms.entity.converter.SavedSearchListConverter;
import net.schwehla.matrosdms.service.message.SavedSearchMessage;

@Entity(name = "Users")
@Table(name = "DBUser", indexes = { @Index(name = "idx_user_uuid", columnList = "uuid") })
@NamedQueries({
		@NamedQuery(name = "DBUser.findAll", query = "SELECT c FROM Users c"),
		@NamedQuery(name = "DBUser.findByUUID", query = "SELECT c FROM Users c where c.uuid = :uuid"),
		@NamedQuery(name = "DBUser.findByNameAndPassword", query = "SELECT c FROM Users c where c.name = :name and c.password = :password")
})
public class DBUser extends AbstractDBInfoBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false, name = "USER_ID")
	private Long id;

	public String password;
	public String email;
	private String firstname;

	@Enumerated(EnumType.STRING)
	@Column(name = "user_role", nullable = false)
	public EUserRole role = EUserRole.USER;

	@Column(columnDefinition = "TEXT")
	@Convert(converter = JpaJsonConverter.class)
	private Map<String, Object> preferences = new HashMap<>();

	@Column(name = "saved_searches", columnDefinition = "TEXT")
	@Convert(converter = SavedSearchListConverter.class)
	private List<SavedSearchMessage> savedSearches = new ArrayList<>();

	// REMOVED: private String refreshToken; (Now handled by DBRefreshToken table)

	public Long getPK() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public EUserRole getRole() {
		return role;
	}

	public void setRole(EUserRole role) {
		this.role = role;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public Map<String, Object> getPreferences() {
		return preferences;
	}

	public void setPreferences(Map<String, Object> preferences) {
		this.preferences = preferences;
	}

	public List<SavedSearchMessage> getSavedSearches() {
		return savedSearches;
	}

	public void setSavedSearches(List<SavedSearchMessage> savedSearches) {
		this.savedSearches = savedSearches;
	}
}