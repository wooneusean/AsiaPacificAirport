import java.util.ArrayList;

public class AirTrafficControllerManager implements Runnable {

    private final Airport airport;
    AirTrafficController atcArrival;
    AirTrafficController atcDeparture;
    ArrayList<ServiceStatistics> statistics = new ArrayList<>();

    public AirTrafficControllerManager(Airport airport) {
        this.airport = airport;
        this.atcArrival = new AirTrafficController(airport, "ATC Arrival");
        this.atcDeparture = new AirTrafficController(airport, "ATC Departure");

        new Thread(atcArrival).start();
        new Thread(atcDeparture).start();
    }

    @Override
    public void run() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        while (
                atcArrival.waitingList.size() > 0 ||
                atcDeparture.waitingList.size() > 0 ||
                airport.runway.isLocked() ||
                airport.gates.availablePermits() < 2
        ) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        Logger.log("ATC Manager", "Seems like it quieted down for the time being, time to perform some sanity checks.");

        Logger.log("ATC Manager", "There are " + airport.gates.availablePermits() + " available gates.");

        int maxLandingWaitingTime =
                statistics.stream().mapToInt(v -> Math.toIntExact(v.landingWaitingTime)).max().orElse(0) / 1000;
        int minLandingWaitingTime =
                statistics.stream().mapToInt(v -> Math.toIntExact(v.landingWaitingTime)).min().orElse(0) / 1000;
        double avgLandingWaitingTime =
                statistics.stream().mapToInt(v -> Math.toIntExact(v.landingWaitingTime)).average().orElse(0) / 1000;

        Logger.log("ATC Manager", String.format(
                "The max/min/avg LANDING waiting time is %ds/%ds/%fs",
                maxLandingWaitingTime,
                minLandingWaitingTime,
                avgLandingWaitingTime
        ));

        int maxTakeOffWaitingTime =
                statistics.stream().mapToInt(v -> Math.toIntExact(v.takeOffWaitingTime)).max().orElse(0) / 1000;
        int minTakeOffWaitingTime =
                statistics.stream().mapToInt(v -> Math.toIntExact(v.takeOffWaitingTime)).min().orElse(0) / 1000;
        double avgTakeOffWaitingTime =
                statistics.stream().mapToInt(v -> Math.toIntExact(v.takeOffWaitingTime)).average().orElse(0) / 1000;

        Logger.log("ATC Manager", String.format(
                "The max/min/avg TAKE-OFF waiting time is %ds/%ds/%fs",
                maxTakeOffWaitingTime,
                minTakeOffWaitingTime,
                avgTakeOffWaitingTime
        ));

        int totalPassengersBoarded = statistics.stream().mapToInt(v -> v.passengersBoarded).sum();

        Logger.log("ATC Manager", "The total number of passengers boarded was " + totalPassengersBoarded);

        Logger.log("ATC Manager", "The total number of planes handled was " + statistics.size());
    }

    void requestLanding(Plane plane) {
        synchronized (atcArrival.waitingList) {
            atcArrival.waitingList.offer(plane);
            atcArrival.waitingList.notifyAll();
        }
    }

    void planeDeparture(Plane plane) {
        synchronized (atcDeparture.waitingList) {
            atcDeparture.waitingList.offer(plane);
            atcDeparture.waitingList.notifyAll();
        }
    }
}
