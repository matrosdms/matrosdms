package net.schwehla.matrosdms.ai.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import net.schwehla.matrosdms.domain.search.MSearchResult;
import net.schwehla.matrosdms.service.SearchService;

class ContextHistoryServiceTest {

	private SearchService searchService;
	private ContextHistoryService service;

	@BeforeEach
	void setUp() {
		searchService = Mockito.mock(SearchService.class);
		service = new ContextHistoryService();
		ReflectionTestUtils.setField(service, "searchService", searchService);
	}

	private MSearchResult withFilename(String name) {
		MSearchResult r = Mockito.mock(MSearchResult.class);
		Mockito.doReturn(name).when(r).getFilename();
		return r;
	}

	@Test
	void filtersBlanksAndLowercases() {
		// Build the row mocks BEFORE the outer when() so their stubbing does not
		// nest inside searchService.search()'s stubbing.
		List<MSearchResult> rows = List.of(
				withFilename("Invoice_2024.PDF"),
				withFilename(null),
				withFilename("   "),
				withFilename("Contract.pdf"));
		when(searchService.search(any(), anyInt(), anyInt())).thenReturn(new PageImpl<>(rows));

		assertThat(service.getRecentFilenames("ctx-1"))
				.containsExactly("invoice_2024.pdf", "contract.pdf");
	}

	@Test
	void emptyResultYieldsEmptyList() {
		when(searchService.search(any(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of()));
		assertThat(service.getRecentFilenames("ctx-1")).isEmpty();
	}
}
