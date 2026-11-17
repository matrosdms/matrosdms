package net.schwehla.matrosdms.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.schwehla.matrosdms.domain.action.EActionPriority;
import net.schwehla.matrosdms.domain.action.EActionStatus;
import net.schwehla.matrosdms.entity.DBAction;
import net.schwehla.matrosdms.service.message.CreateActionMessage;

class MActionMapperTest {

	private final MActionMapper mapper = new MActionMapperImpl();

	@Test
	void createWithoutStatusDefaultsToOpen() {
		// The inbox follow-up reminder sends no status; the action table's
		// status column is NOT NULL.
		CreateActionMessage msg = new CreateActionMessage();
		msg.setName("Follow up");

		DBAction action = mapper.modelToEntity(msg);

		assertThat(action.getStatus()).isEqualTo(EActionStatus.OPEN);
	}

	@Test
	void createWithNullPriorityDefaultsToNormal() {
		CreateActionMessage msg = new CreateActionMessage();
		msg.setName("Follow up");
		msg.setPriority(null);

		DBAction action = mapper.modelToEntity(msg);

		assertThat(action.getPriority()).isEqualTo(EActionPriority.NORMAL);
	}

	@Test
	void createKeepsExplicitStatusAndPriority() {
		CreateActionMessage msg = new CreateActionMessage();
		msg.setName("Follow up");
		msg.setStatus(EActionStatus.IN_PROGRESS);
		msg.setPriority(EActionPriority.HIGH);

		DBAction action = mapper.modelToEntity(msg);

		assertThat(action.getStatus()).isEqualTo(EActionStatus.IN_PROGRESS);
		assertThat(action.getPriority()).isEqualTo(EActionPriority.HIGH);
	}
}
