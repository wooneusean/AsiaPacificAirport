public class ServiceStatistics {
    static int planesHandled = 0;
    long landingWaitingTime;
    long takeOffWaitingTime;
    int passengersBoarded;
    Plane plane;

    public ServiceStatistics(long landingWaitingTime, long takeOffWaitingTime, int passengersBoarded, Plane plane) {
        this.landingWaitingTime = landingWaitingTime;
        this.takeOffWaitingTime = takeOffWaitingTime;
        this.passengersBoarded = passengersBoarded;
        this.plane = plane;
    }
}
