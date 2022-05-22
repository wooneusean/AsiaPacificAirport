import java.util.LinkedList;
import java.util.Queue;

public class AirTrafficController implements Runnable {
    final Queue<Plane> waitingList = new LinkedList<>();

    private final Airport airport;

    String name;

    public AirTrafficController(Airport airport, String name) {
        this.airport = airport;
        this.name = name;
    }

    @Override
    public void run() {
        Logger.log(name, "Reporting for duty!");

        Plane plane;
        while (true) {
            synchronized (waitingList) {
                while (waitingList.size() == 0) {
                    try {
                        waitingList.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                plane = waitingList.peek();
            }

            if (plane.isArriving.get()) {
                handleArrival(plane);
            } else {
                handleDeparture(plane);
            }
        }
    }

    private void handleArrival(Plane plane) {
        Logger.log(name, "Looking for available GATES for " + plane.callSign);

        synchronized (airport.gates) {
            while (airport.gates.availablePermits() == 0) {
                Logger.log(name, "There are no available gates, please continue holding " + plane.callSign);
                try {
                    airport.gates.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Logger.log(name, "Found GATE for " + plane.callSign);

        Logger.log(name, "Making sure RUNWAY is clear for " + plane.callSign);

        synchronized (airport.runway) {
            while (airport.runway.isLocked()) {
                Logger.log(name, "RUNWAY is occupied, please hold " + plane.callSign);
                try {
                    airport.runway.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Logger.log(name, String.format("RUNWAY is clear, %s is granted to land.", plane.callSign));

        synchronized (plane.clearToLand) {
            plane.clearToLand.set(true);
            plane.clearToLand.notifyAll();
        }

        synchronized (waitingList) {
            waitingList.poll();
            waitingList.notifyAll();
        }
    }

    private void handleDeparture(Plane plane) {
        Logger.log(name, String.format("Roger that %s, making sure RUNWAY is unoccupied...", plane.callSign));

        synchronized (airport.runway) {
            while (airport.runway.isLocked()) {
                Logger.log(name, "RUNWAY is occupied, please wait " + plane.callSign);
                try {
                    airport.runway.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Logger.log(name, "RUNWAY is free, proceed to depart " + plane.callSign);

        synchronized (plane.clearToDepart) {
            plane.clearToDepart.set(true);
            plane.clearToDepart.notifyAll();
        }

        synchronized (waitingList) {
            waitingList.poll();
            waitingList.notifyAll();
        }
    }
}
