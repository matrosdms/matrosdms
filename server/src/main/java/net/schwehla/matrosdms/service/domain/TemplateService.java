/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.admin.EConfigKey;
import net.schwehla.matrosdms.domain.template.TemplateManifest;
import net.schwehla.matrosdms.domain.template.TemplateProposal;

@Service
@Lazy
public class TemplateService {

	private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

	@Autowired
	AppServerSpringConfig appConfig;
	@Autowired
	ConfigService configService;
	@Autowired
	ObjectMapper jsonMapper;

	private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	public List<TemplateProposal> getAllProposals() {
		List<TemplateProposal> result = new ArrayList<>();
		String basePath = getBasePath();

		try {
			Resource[] resources = resolver.getResources(basePath + "*/manifest.json");
			for (Resource r : resources) {
				try {
					TemplateManifest m = jsonMapper.readValue(r.getInputStream(), TemplateManifest.class);
					TemplateProposal proposal = new TemplateProposal();
					proposal.setId(m.getId());
					proposal.setName(m.getName());
					proposal.setDescription(m.getDescription());
					proposal.setAvailableLanguages(m.getSupportedLanguages());
					result.add(proposal);
				} catch (Exception e) {
					log.error("Invalid manifest: {}", r.getFilename());
				}
			}
		} catch (IOException e) {
			log.error("Error scanning templates", e);
		}
		return result;
	}

	public String getTemplateContent(String templateId, String language) {
		String lang = (language != null) ? language : configService.getValue(EConfigKey.PREFERED_LANGUAGE, "en");
		String basePath = getBasePath();

		try {
			Resource[] resources = resolver.getResources(basePath + templateId + "/tree_" + lang + ".yaml");
			if (resources.length == 0) {
				// Fallback to English
				resources = resolver.getResources(basePath + templateId + "/tree_en.yaml");
			}

			if (resources.length > 0) {
				return StreamUtils.copyToString(resources[0].getInputStream(), StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			log.error("Failed to load template content", e);
		}
		return "";
	}

	private String getBasePath() {
		String path = appConfig.getTemplates().getLocation();
		return path.endsWith("/") ? path : path + "/";
	}
}
