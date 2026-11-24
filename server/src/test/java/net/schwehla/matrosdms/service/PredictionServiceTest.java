/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

import net.schwehla.matrosdms.ai.IPredictionStrategy;
import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.config.model.AppServerSpringConfig.StrategyConfig;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates.Candidate;
import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.repository.CategoryRepository;
import net.schwehla.matrosdms.service.domain.ContextService;
import net.schwehla.matrosdms.service.message.DigestResultMessage;

class PredictionServiceTest {

	@Test
	void kindCandidatesAreTheCategoriesBelowTheKindRootNotTheRootItself() {
		AtomicReference<ClassificationCandidates> seen = new AtomicReference<>();
		IPredictionStrategy strategy = new IPredictionStrategy() {
			@Override
			public void analyze(String text, String filename, ClassificationCandidates c, DigestResultMessage r) {
				seen.set(c);
			}

			@Override
			public String getId() {
				return "heuristic";
			}
		};

		PredictionService service = new PredictionService();
		service.strategies = List.of(strategy);
		service.contextService = Mockito.mock(ContextService.class);
		service.appConfig = Mockito.mock(AppServerSpringConfig.class, Answers.RETURNS_DEEP_STUBS);
		when(service.appConfig.getAi().getClassification().getHeuristic()).thenReturn(new StrategyConfig());
		service.categoryRepository = Mockito.mock(CategoryRepository.class);
		when(service.categoryRepository.findAll()).thenReturn(List.of(
				category("ROOT_KIND", "KIND", "Document Type"),
				category("48AD2UA5G87", "Beleg", null),
				category("48AD2UATU59", "Rechnung", null),
				category("48TRD8944QU", "Wohnen", null)));
		service.categoryLookupService = Mockito.mock(CategoryLookupService.class);
		when(service.categoryLookupService.getRootFor(anyString()))
				.thenAnswer(inv -> "48TRD8944QU".equals(inv.getArgument(0)) ? "ROOT_WHAT" : "ROOT_KIND");

		service.predictMetadata("<!DOCTYPE html> document type", "mail.eml", new DigestResultMessage());

		assertThat(seen.get().kinds()).extracting(Candidate::name).containsExactly("Beleg", "Rechnung");
	}

	@Test
	void onlyTheKindTreeCounts() {
		assertThat(PredictionService.isDocumentKind("48AD2UATU59", "ROOT_KIND")).isTrue();
		assertThat(PredictionService.isDocumentKind("ROOT_KIND", "ROOT_KIND")).isFalse();
		assertThat(PredictionService.isDocumentKind("48TRD8944QU", "ROOT_WHAT")).isFalse();
		assertThat(PredictionService.isDocumentKind("48AD2UATU59", null)).isFalse();
	}

	private static DBCategory category(String uuid, String name, String description) {
		DBCategory c = new DBCategory();
		c.setUuid(uuid);
		c.setName(name);
		c.setDescription(description);
		return c;
	}
}
