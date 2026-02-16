/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportItemMetadata {
    public String uuid;
    public String name;
    public String description;
    public String originalFilename;
    public String filename;
    public String context;
    public String store;
    public String dateIssued;
    public String dateCreated;
    public List<String> tags = new ArrayList<>();
    
    // Human readable attribute map (Key Name -> Value)
    public Map<String, Object> attributes = new HashMap<>(); 
    
    // System fields
    public String source;
    public String sha256;
}