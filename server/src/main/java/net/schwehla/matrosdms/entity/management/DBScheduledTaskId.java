package net.schwehla.matrosdms.entity.management;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DBScheduledTaskId implements Serializable {

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(name = "task_instance", nullable = false)
    private String taskInstance;

    public DBScheduledTaskId() {}

    public DBScheduledTaskId(String taskName, String taskInstance) {
        this.taskName = taskName;
        this.taskInstance = taskInstance;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskInstance() {
        return taskInstance;
    }

    public void setTaskInstance(String taskInstance) {
        this.taskInstance = taskInstance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DBScheduledTaskId)) return false;
        DBScheduledTaskId that = (DBScheduledTaskId) o;
        return Objects.equals(taskName, that.taskName)
                && Objects.equals(taskInstance, that.taskInstance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskName, taskInstance);
    }
}
