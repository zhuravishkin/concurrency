package course.concurrency.exams.refactoring;

public class UpdateTask {

    private final Others.MountTableManager manager;
    private final String adminAddress;
    private boolean isDone;

    public UpdateTask(Others.MountTableManager manager,

                      String adminAddress) {
        this.manager = manager;
        this.adminAddress = adminAddress;
    }

    public String getAdminAddress() {
        return adminAddress;
    }

    public boolean isDone() {
        return isDone;
    }

    public void process() {
        isDone = manager.refresh();
    }
}
