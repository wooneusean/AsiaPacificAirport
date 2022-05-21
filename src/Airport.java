import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class Airport {
    final Queue<Plane> waitingList = new LinkedList<>();
    final Semaphore gates = new Semaphore(2);
    final ReentrantLock runway = new ReentrantLock();
    String name;

    RefillTruck refillTruck = new RefillTruck("Refill Truck");

    AirTrafficControl atc = new AirTrafficControl(this);

    Airport(String name) {
        this.name = name;
        new Thread(refillTruck).start();
    }

    public static void main(String[] args) {
        Airport apa = new Airport("Asia Pacific Airport");

        PlaneGenerator pg = new PlaneGenerator(apa, 6);
        Thread pgThread = new Thread(pg);
        pgThread.start();
    }

    void queueUp(Plane plane) {
        Logger.log("ATC", plane.callSign + " is queuing!");
        waitingList.offer(plane);
    }
}
