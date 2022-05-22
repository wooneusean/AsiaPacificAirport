import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class Airport {
    final Semaphore gates = new Semaphore(2);

    final ReentrantLock runway = new ReentrantLock();

    String name;

    RefillTruck refillTruck = new RefillTruck("Refill Truck");

    AirTrafficControllerManager atcManager = new AirTrafficControllerManager(this);

    CleanUpCrewManager ccManager = new CleanUpCrewManager();

    Airport(String name) {
        this.name = name;
        new Thread(refillTruck).start();
        new Thread(atcManager).start();
        new Thread(ccManager).start();
    }

    public static void main(String[] args) {
        Airport apa = new Airport("Asia Pacific Airport");

        PlaneGenerator pg = new PlaneGenerator(apa, 6);
        Thread pgThread = new Thread(pg);
        pgThread.start();
    }
}
