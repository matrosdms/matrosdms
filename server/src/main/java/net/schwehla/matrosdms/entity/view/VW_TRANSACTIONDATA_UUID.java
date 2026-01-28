/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity.view;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

/*
@Entity
@Table(name = "VW_TRANSACTIONDATA_UUID")

@NamedQueries({
        @NamedQuery(name = "VW_TRANSACTIONDATA_UUID.findAll", query = "SELECT c FROM VW_TRANSACTIONDATA_UUID c"), })
*/

public class VW_TRANSACTIONDATA_UUID {

	@Column(unique = false, nullable = false, name = "ID")
	private Long ID;

	@Column(unique = false, nullable = false, updatable = false, name = "TAB_TYPE")
	private String TYPE;

	@Id
	@Column(unique = true, nullable = false, updatable = false)
	private String UUID;

	public Long getID() {
		return ID;
	}

	public void setID(Long iD) {
		ID = iD;
	}

	public String getTYPE() {
		return TYPE;
	}

	public void setTYPE(String tYPE) {
		TYPE = tYPE;
	}

	public String getUUID() {
		return UUID;
	}

	public void setUUID(String uUID) {
		UUID = uUID;
	}
}
