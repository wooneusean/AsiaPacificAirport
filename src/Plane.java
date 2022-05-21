import java.util.concurrent.ThreadLocalRandom;

public class Plane implements Runnable {
    Airport airport;

    int maxPassengers;

    int numPassengers;

    int fuelLevel;

    int cleanlinessLevel;

    String callSign;

    Boolean clearToLand = false;

    Plane(Airport airport) {
        this.airport = airport;
        this.numPassengers = ThreadLocalRandom.current().nextInt(5, 10/*50*/);
        this.maxPassengers = this.numPassengers;
        this.fuelLevel = ThreadLocalRandom.current().nextInt(25, 75);
        this.cleanlinessLevel = ThreadLocalRandom.current().nextInt(25, 75);

        StringBuilder sb = new StringBuilder();
        callSign = sb.append((char) ThreadLocalRandom.current().nextInt('A', 'Z'))
                     .append((char) ThreadLocalRandom.current().nextInt('A', 'Z'))
                     .append((char) ThreadLocalRandom.current().nextInt('A', 'Z'))
                     .append("-")
                     .append(ThreadLocalRandom.current().nextInt(100, 999))
                     .toString();

        this.airport.queueUp(this);
    }

    void disembarkPassengers() {
        for (int i = numPassengers; i > 1; i--) {
            Logger.log(callSign, "DISEMBARKING passenger #" + i);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void embarkPassengers() {
        for (int i = 1; i < maxPassengers; i++) {
            Logger.log(callSign, "EMBARKING passenger #" + i);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void run() {
        synchronized (airport.waitingList) {
            while (airport.waitingList.peek() != this) {
                try {
                    airport.waitingList.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Logger.log("ATC", "Looking for available GATES for " + callSign);

        synchronized (airport.gates) {
            while (airport.gates.availablePermits() == 0) {
                try {
                    airport.gates.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Logger.log("ATC", "Found GATE for " + callSign);

        Logger.log("ATC", "Making sure RUNWAY is clear for " + callSign);

        synchronized (airport.runway) {
            while (airport.runway.isLocked()) {
                try {
                    airport.runway.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Logger.log("ATC", String.format("RUNWAY is clear, %s is granted to land.", callSign));

        Logger.log(callSign, "Roger that, proceeding to LAND.");
        airport.runway.lock();
        Logger.log(callSign, "Touching down...");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        int assignedGate = airport.gates.availablePermits();
        Logger.log(callSign, "Touch down! Safely LANDED.");

        try {
            airport.gates.acquire(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        synchronized (airport.runway) {
            airport.runway.unlock();
            airport.runway.notifyAll();
        }

        synchronized (airport.waitingList) {
            airport.waitingList.poll();
            airport.waitingList.notifyAll();
        }

        Logger.log(callSign, "TAXIING to gate #" + assignedGate + "...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Logger.log(callSign, "TAXIED to gate #" + assignedGate);

        Logger.log(callSign, "Starting the DOCKING process at gate #" + assignedGate);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Logger.log(callSign, "DOCKED to gate #" + assignedGate);

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

        synchronized (this) {
            while (fuelLevel != 100) {
                Logger.log(callSign, "Waiting on REFUELLING...");
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Logger.log(callSign, "Requesting for DEPARTURE.");

        synchronized (airport.runway) {
            while (airport.runway.isLocked()) {
                Logger.log("ATC", "RUNWAY is occupied, please wait.");
                try {
                    airport.runway.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Logger.log("ATC", "RUNWAY is free, proceed to depart " + callSign);


        Logger.log(callSign, "UNDOCKING from gate #" + assignedGate);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Logger.log(callSign, "Done UNDOCKING from gate #" + assignedGate);

        airport.runway.lock();

        Logger.log(callSign, "TAXIING to runway...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Logger.log(callSign, "TAXIED to runway.");

        Logger.log(callSign, "Taking off!");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Logger.log(callSign, "Took off, bye bye, " + airport.name);

        synchronized (airport.runway) {
            airport.runway.unlock();
            airport.runway.notifyAll();
        }
        synchronized (airport.gates) {
            airport.gates.release(1);
            airport.gates.notifyAll();
        }
    }
}
