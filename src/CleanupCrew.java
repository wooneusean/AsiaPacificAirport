public class CleanupCrew implements Runnable {

    private final Plane plane;

    String name;

    CleanupCrew(Plane plane) {
        this.name = plane.callSign + " Cleanup Crew";
        this.plane = plane;
    }

    @Override
    public void run() {
        cleanPlane();
    }

    void cleanPlane() {
        while (plane.cleanlinessLevel < 100) {
            plane.cleanlinessLevel = Math.min(plane.cleanlinessLevel + 5, 100);
            Logger.log(name, String.format("Cleaning up %s. %d%% cleaned.", plane.callSign, plane.cleanlinessLevel));

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
