/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity.management;

import java.time.Instant;

import jakarta.persistence.*;

import net.schwehla.matrosdms.entity.DBBaseEntity;

@Entity
@Table(name = "refresh_token")
public class DBRefreshToken extends DBBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refresh_token_seq_gen")
	@SequenceGenerator(name = "refresh_token_seq_gen", sequenceName = "refresh_token_seq", allocationSize = 50)
	private Long id;

	@Column(nullable = false, unique = true)
	private String token;

	@Column(nullable = false, name = "expiry_date")
	private Instant expiryDate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private DBUser user;

	public DBRefreshToken() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Instant getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Instant expiryDate) {
		this.expiryDate = expiryDate;
	}

	public DBUser getUser() {
		return user;
	}

	public void setUser(DBUser user) {
		this.user = user;
	}
}