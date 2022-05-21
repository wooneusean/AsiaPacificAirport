import java.util.concurrent.ThreadLocalRandom;

public class PlaneGenerator implements Runnable {
    private final Airport apa;
    private final int quantity;

    PlaneGenerator(Airport apa, int quantity) {
        this.apa = apa;
        this.quantity = quantity;
    }

    @Override
    public void run() {
        for (int i = 0; i < quantity; i++) {
            Plane plane = new Plane(apa);
            Thread planeThread = new Thread(plane);
            planeThread.start();

            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 3000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
