import java.util.LinkedList;
import java.util.Queue;

public class RefillTruck implements Runnable {
    final Queue<Plane> waitingList = new LinkedList<>();

    String name;

    Plane plane;

    public RefillTruck(String name) {
        Logger.log(name, "Reporting for duty.");
        this.name = name;
    }

    @Override
    public void run() {
        while (true) {
            synchronized (waitingList) {
                while (waitingList.size() == 0) {
                    Logger.log(name, "Waiting for planes to refill.");
                    try {
                        waitingList.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            plane = waitingList.poll();

            synchronized (plane) {
                while (plane.fuelLevel < 100) {
                    plane.fuelLevel = Math.min(plane.fuelLevel + 5, 100);

                    Logger.log(name, String.format("REFUELLING %s. %d%% full.", plane.callSign, plane.fuelLevel));

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                plane.notifyAll();
            }
        }
    }

    void refuelPlane(Plane plane) {
        synchronized (waitingList) {
            waitingList.offer(plane);
            if (waitingList.size() == 1) {
                waitingList.notify();
            }
        }
    }
}
