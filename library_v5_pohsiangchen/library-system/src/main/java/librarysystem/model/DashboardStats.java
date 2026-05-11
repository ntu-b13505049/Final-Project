package librarysystem.model;

public class DashboardStats {
    private int totalBooks;
    private int activeUsers;
    private int currentBorrows;
    private int overdueBorrows;
    private int totalReviews;
    private int waitingReservations;
    private int pendingRoleRequests;

    public int getTotalBooks() {
        return totalBooks;
    }

    public void setTotalBooks(int totalBooks) {
        this.totalBooks = totalBooks;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public int getCurrentBorrows() {
        return currentBorrows;
    }

    public void setCurrentBorrows(int currentBorrows) {
        this.currentBorrows = currentBorrows;
    }

    public int getOverdueBorrows() {
        return overdueBorrows;
    }

    public void setOverdueBorrows(int overdueBorrows) {
        this.overdueBorrows = overdueBorrows;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public int getWaitingReservations() {
        return waitingReservations;
    }

    public void setWaitingReservations(int waitingReservations) {
        this.waitingReservations = waitingReservations;
    }

    public int getPendingRoleRequests() {
        return pendingRoleRequests;
    }

    public void setPendingRoleRequests(int pendingRoleRequests) {
        this.pendingRoleRequests = pendingRoleRequests;
    }
}
