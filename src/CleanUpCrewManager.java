import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class CleanUpCrewManager implements Runnable {
    final Queue<Plane> waitingList = new LinkedList<>();

    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    @Override
    public void run() {
        while (true) {
            synchronized (waitingList) {
                while (waitingList.size() == 0) {
                    try {
                        waitingList.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            while (!waitingList.isEmpty()) {
                Plane plane = waitingList.poll();
                executor.submit(() -> {
                    while (plane.cleanlinessLevel < 100) {
                        plane.cleanlinessLevel = Math.min(plane.cleanlinessLevel + 5, 100);
                        Logger.log(
                                "Cleanup Crew (" + plane.callSign + ")",
                                String.format(
                                        "Cleaning up %s. %d%% cleaned.",
                                        plane.callSign,
                                        plane.cleanlinessLevel
                                )
                        );

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        }
    }

    void cleanPlane(Plane plane) {
        synchronized (waitingList) {
            waitingList.offer(plane);
            waitingList.notifyAll();
        }
    }
}
