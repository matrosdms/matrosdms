package net.schwehla.matrosdms.ai.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates.Candidate;
import net.schwehla.matrosdms.domain.ai.OllamaResponse;
import net.schwehla.matrosdms.service.message.DigestResultMessage;

class OllamaPredictionStrategyTest {

	private RestTemplate restTemplate;
	private OllamaPredictionStrategy strategy;

	private final Candidate ctx = new Candidate("ctx-1", "Insurance", "");
	private final Candidate kind = new Candidate("kind-1", "Invoice", "");

	@BeforeEach
	void setUp() throws Exception {
		restTemplate = Mockito.mock(RestTemplate.class);
		strategy = new OllamaPredictionStrategy(restTemplate, new ObjectMapper());

		AppServerSpringConfig cfg = new AppServerSpringConfig();
		cfg.getAi().setConcurrency(1);
		cfg.getAi().getClassification().getOllama().setUrl("http://localhost:11434");
		cfg.getAi().getClassification().getOllama().setModel("llama3");

		set(strategy, "appConfig", cfg);
		set(strategy, "promptTemplate", "%s %s %s %s");
		set(strategy, "gpuLock", new Semaphore(1));
	}

	private void set(Object target, String field, Object value) throws Exception {
		Field f = target.getClass().getDeclaredField(field);
		f.setAccessible(true);
		f.set(target, value);
	}

	private void stubOllama(String rawResponse) {
		OllamaResponse resp = new OllamaResponse();
		resp.setResponse(rawResponse);
		when(restTemplate.postForEntity(anyString(), any(), eq(OllamaResponse.class)))
				.thenReturn(ResponseEntity.ok(resp));
	}

	private DigestResultMessage analyze() {
		DigestResultMessage result = new DigestResultMessage();
		strategy.analyze("some document text", "file.pdf",
				new ClassificationCandidates(List.of(ctx), List.of(kind)), result);
		return result;
	}

	@Test
	void mapsValidJsonWithKnownUuids() {
		stubOllama("{\"contextUuid\":\"ctx-1\",\"kindUuid\":\"kind-1\",\"date\":\"2024-05-01\","
				+ "\"summary\":\"An invoice\",\"confidence\":0.9}");
		var p = analyze().getPrediction();
		assertThat(p.getContext()).isEqualTo("ctx-1");
		assertThat(p.getKind()).isEqualTo("kind-1");
		assertThat(p.getDocumentDate()).isEqualTo(LocalDate.of(2024, 5, 1));
		assertThat(p.getSummary()).isEqualTo("An invoice");
		assertThat(p.getConfidence()).isEqualTo(0.9);
	}

	@Test
	void discardsHallucinatedUuidButKeepsOtherFields() {
		stubOllama("{\"contextUuid\":\"ctx-DOES-NOT-EXIST\",\"kindUuid\":\"kind-1\","
				+ "\"summary\":\"hi\",\"confidence\":0.7}");
		var p = analyze().getPrediction();
		assertThat(p.getContext()).isNull();           // hallucinated -> dropped
		assertThat(p.getKind()).isEqualTo("kind-1");   // valid -> kept
		assertThat(p.getSummary()).isEqualTo("hi");
	}

	@Test
	void clampsOutOfRangeConfidence() {
		stubOllama("{\"kindUuid\":\"kind-1\",\"confidence\":87}");
		assertThat(analyze().getPrediction().getConfidence()).isEqualTo(1.0);
	}

	@Test
	void extractsJsonWrappedInProse() {
		stubOllama("Sure! Here is the result:\n```json\n{\"kindUuid\":\"kind-1\"}\n```\nHope that helps.");
		assertThat(analyze().getPrediction().getKind()).isEqualTo("kind-1");
	}

	@Test
	void doesNotCrashOnUnparseableResponse() {
		stubOllama("this is not json at all");
		var p = analyze().getPrediction();
		assertThat(p.getContext()).isNull();
		assertThat(p.getKind()).isNull();
	}

	@Test
	void takesOverTheTitleWithoutQuotesOrFileExtension() {
		stubOllama("{\"kindUuid\":\"kind-1\",\"title\":\" \\\"VBL Renteninformation 2014.pdf\\\" \",\"confidence\":0.9}");
		var p = analyze().getPrediction();
		assertThat(p.getTitle()).isEqualTo("VBL Renteninformation 2014");
		assertThat(p.getFieldConfidences()).containsEntry("title", 0.9);
	}

	@Test
	void noTitleMeansNoTitleConfidence() {
		stubOllama("{\"kindUuid\":\"kind-1\",\"title\":null,\"confidence\":0.9}");
		var p = analyze().getPrediction();
		assertThat(p.getTitle()).isNull();
		assertThat(p.getFieldConfidences()).doesNotContainKey("title");
	}

	@Test
	void whatTheMailItselfSaysStaysWhenTheLlmHasNothingBetter() {
		stubOllama("{\"kindUuid\":\"kind-1\",\"date\":\"2020-01-01\",\"customAttributes\":{\"ATTR_AMOUNT\":12.5}}");
		DigestResultMessage result = new DigestResultMessage();
		result.getPrediction().setTitle("Termin Zahnarzt");
		result.getPrediction().setDocumentDate(LocalDate.of(2024, 3, 5));
		result.getPrediction().setAttributes(new java.util.HashMap<>(java.util.Map.of("SENDER", "praxis@example.org")));

		strategy.analyze("some document text", "mail.eml", new ClassificationCandidates(List.of(ctx), List.of(kind)),
				result);

		var p = result.getPrediction();
		assertThat(p.getTitle()).isEqualTo("Termin Zahnarzt");
		assertThat(p.getDocumentDate()).isEqualTo(LocalDate.of(2024, 3, 5));
		assertThat(p.getAttributes()).containsEntry("SENDER", "praxis@example.org").containsEntry("ATTR_AMOUNT", 12.5);
	}

	@Test
	void theLlmNeverSeesTheDayTheFileWasRead() {
		stubOllama("{\"kindUuid\":\"kind-1\"}");
		var prompt = org.mockito.ArgumentCaptor.forClass(Object.class);

		strategy.analyze("<meta>\n    <processed_date>2026-11-18</processed_date>\n</meta>\nBrief vom 05.03.2024",
				"scan.pdf", new ClassificationCandidates(List.of(ctx), List.of(kind)), new DigestResultMessage());

		Mockito.verify(restTemplate).postForEntity(anyString(), prompt.capture(), eq(OllamaResponse.class));
		String sent = ((net.schwehla.matrosdms.domain.ai.OllamaRequest) prompt.getValue()).prompt();
		assertThat(sent).doesNotContain("2026-11-18").doesNotContain("processed_date").contains("05.03.2024");
	}

	@Test
	void aTitleIsOneLineAndNeverTooLong() {
		assertThat(OllamaPredictionStrategy.cleanTitle(null)).isNull();
		assertThat(OllamaPredictionStrategy.cleanTitle("  ")).isNull();
		assertThat(OllamaPredictionStrategy.cleanTitle("null")).isNull();
		assertThat(OllamaPredictionStrategy.cleanTitle("„Allianz Beitragsanpassung“")).isEqualTo("Allianz Beitragsanpassung");
		assertThat(OllamaPredictionStrategy.cleanTitle("Stadtwerke\nJahresabrechnung  2023")).isEqualTo("Stadtwerke Jahresabrechnung 2023");
		// abbreviations are not file extensions
		assertThat(OllamaPredictionStrategy.cleanTitle("Kündigung z.Hd. Frau Meier")).isEqualTo("Kündigung z.Hd. Frau Meier");
		assertThat(OllamaPredictionStrategy.cleanTitle("Vertrag Nr. 2024.11")).isEqualTo("Vertrag Nr. 2024.11");
		assertThat(OllamaPredictionStrategy.cleanTitle("x".repeat(300))).hasSize(OllamaPredictionStrategy.MAX_TITLE);
	}

	@Test
	void ignoresInvalidDate() {
		stubOllama("{\"kindUuid\":\"kind-1\",\"date\":\"not-a-date\"}");
		assertThat(analyze().getPrediction().getDocumentDate()).isNull();
	}

	@Test
	void survivesNullResponseBody() {
		when(restTemplate.postForEntity(anyString(), any(), eq(OllamaResponse.class)))
				.thenReturn(ResponseEntity.ok(null));
		// must not throw, and must release the semaphore
		analyze();
	}
}
