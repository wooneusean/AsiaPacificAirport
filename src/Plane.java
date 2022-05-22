import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class Plane implements Runnable {
    final AtomicBoolean clearToLand = new AtomicBoolean(false);
    final AtomicBoolean clearToDepart = new AtomicBoolean(false);
    final AtomicBoolean isArriving = new AtomicBoolean(true);
    Airport airport;
    int maxPassengers;
    int numPassengers;
    int fuelLevel;
    int cleanlinessLevel;
    String callSign;

    Plane(Airport airport) {
        this.airport = airport;
        this.numPassengers = ThreadLocalRandom.current().nextInt(5, 50);
        this.maxPassengers = this.numPassengers;
        this.fuelLevel = ThreadLocalRandom.current().nextInt(25,75);
        this.cleanlinessLevel = ThreadLocalRandom.current().nextInt(25, 75);

        StringBuilder sb = new StringBuilder();
        callSign = sb.append((char) ThreadLocalRandom.current().nextInt('A', 'Z'))
                     .append((char) ThreadLocalRandom.current().nextInt('A', 'Z'))
                     .append((char) ThreadLocalRandom.current().nextInt('A', 'Z'))
                     .append("-")
                     .append(ThreadLocalRandom.current().nextInt(100, 999))
                     .toString();
    }

    void disembarkPassengers() {
        for (int i = numPassengers; i >= 0; i--) {
            Logger.log(callSign, "DISEMBARKING passenger #" + (i + 1));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void embarkPassengers() {
        for (int i = 0; i <= maxPassengers; i++) {
            Logger.log(callSign, "EMBARKING passenger #" + (i + 1));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void run() {
        Logger.log(callSign, "Queued up and requesting for landing!");

        airport.atcManager.requestLanding(this);

        long landingWaitingTimeStart = System.currentTimeMillis();

        synchronized (clearToLand) {
            while (!clearToLand.get()) {
                Logger.log(callSign, "Waiting for clearance to LAND.");
                try {
                    clearToLand.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            airport.runway.lock();
            airport.gates.acquire(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        long landingWaitingTime = System.currentTimeMillis() - landingWaitingTimeStart;

        Logger.log(callSign, "Roger that, proceeding to LAND.");

        Logger.log(callSign, "Touching down...");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Logger.log(callSign, "Touch down! Safely LANDED.");

        synchronized (airport.runway) {
            airport.runway.unlock();
            airport.runway.notifyAll();
        }

        Logger.log(callSign, "TAXIING to assigned gate..");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Logger.log(callSign, "TAXIED to assigned gate");

        Logger.log(callSign, "DOCKING to assigned gate...");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Logger.log(callSign, "Successfully DOCKED");

        CleanupCrew cleanupCrew = new CleanupCrew(this);
        Thread cleanupCrewThread = new Thread(cleanupCrew);
        cleanupCrewThread.start();
        airport.refillTruck.refuelPlane(this);

        disembarkPassengers();

        embarkPassengers();

        try {
            cleanupCrewThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        while (fuelLevel < 100) {
            Logger.log(callSign, "Plane not refueled yet, waiting on REFUELLING...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        Logger.log(callSign, "All green across the board, ready to depart.");

        Logger.log(callSign, "Requesting for clearance to DEPART.");

        isArriving.set(false);

        airport.atcManager.planeDeparture(this);

        long takeOffWaitingTimeStart = System.currentTimeMillis();

        synchronized (clearToDepart) {
            while (!clearToDepart.get()) {
                try {
                    clearToDepart.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        long takeOffWaitingTime = System.currentTimeMillis() - takeOffWaitingTimeStart;

        airport.runway.lock();

        Logger.log(callSign, "UNDOCKING from assigned gate...");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Logger.log(callSign, "Done UNDOCKING from assigned gate");

        Logger.log(callSign, "TAXIING to runway...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Logger.log(callSign, "TAXIED to runway.");

        Logger.log(callSign, "Taking off!");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Logger.log(callSign, "Took off. Bye bye, " + airport.name);

        synchronized (airport.runway) {
            airport.runway.unlock();
            airport.runway.notifyAll();
        }

        synchronized (airport.gates) {
            airport.gates.release(1);
            airport.gates.notifyAll();
        }

        ServiceStatistics stats = new ServiceStatistics(landingWaitingTime, takeOffWaitingTime, maxPassengers, this);
        airport.atcManager.statistics.add(stats);
        ServiceStatistics.planesHandled++;
    }
}
