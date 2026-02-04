/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity.management;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import net.schwehla.matrosdms.entity.DBBaseEntity;

@Entity
@Table(name = "CONFIG", indexes = @jakarta.persistence.Index(name = "idx_config_key", columnList = "config_key"))
@NamedQueries({
		@NamedQuery(name = "DBConfig.findAll", query = "SELECT c FROM DBConfig c"),
		@NamedQuery(name = "DBConfig.findByKey", query = "SELECT c FROM DBConfig c where c.config_key = :key ")
})
public class DBConfig extends DBBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false, name = "CONFIG_ID")
	private Long id;

	String config_key;

	String config_value;

	public String getConfig_key() {
		return config_key;
	}

	public void setConfig_key(String config_key) {
		this.config_key = config_key;
	}

	public String getConfig_value() {
		return config_value;
	}

	public void setConfig_value(String config_value) {
		this.config_value = config_value;
	}

	public Long getId() {
		return id;
	}
}
