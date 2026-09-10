package com.example;

import java.util.*;

public class App {

    // ---------- Custom Exceptions ----------
    public static class InvalidRequestException extends Exception {
        public InvalidRequestException(String msg) { super(msg); }
    }

    public static class NoAmbulanceAvailableException extends Exception {
        public NoAmbulanceAvailableException(String msg) { super(msg); }
    }

    // ---------- Enums ----------
    public enum EmergencyLevel { CRITICAL, HIGH, MODERATE, NORMAL }

    public enum AmbulanceType { BASIC, ALS, ICU }

    public enum AmbulanceState {
        AVAILABLE, DISPATCHED, EN_ROUTE, PATIENT_PICKED_UP, HOSPITAL_ARRIVED
    }

    // ---------- Ambulance ----------
    public static class Ambulance {
        String id;
        AmbulanceType type;
        AmbulanceState state;
        String driverName;

        public Ambulance(String id, AmbulanceType type, String driverName) {
            this.id = id;
            this.type = type;
            this.driverName = driverName;
            this.state = AmbulanceState.AVAILABLE;
        }

        public boolean isAvailable() {
            return state == AmbulanceState.AVAILABLE;
        }
    }

    // ---------- Emergency Request ----------
    public static class EmergencyRequest {
        String patientId;
        EmergencyLevel level;
        String pickupLocation;
        String destinationHospital;
        double distanceKm;
        Ambulance assignedAmbulance;
        String status;

        public EmergencyRequest(String patientId, EmergencyLevel level,
                                String pickup, String hospital, double distanceKm) {
            this.patientId = patientId;
            this.level = level;
            this.pickupLocation = pickup;
            this.destinationHospital = hospital;
            this.distanceKm = distanceKm;
            this.status = "PENDING";
        }
    }

    // ---------- Dispatch System ----------
    private List<Ambulance> fleet = new ArrayList<>();
    private Queue<EmergencyRequest> waitingQueue = new LinkedList<>();
    private List<EmergencyRequest> history = new ArrayList<>();

    public void addAmbulance(Ambulance a) {
        fleet.add(a);
    }

    public List<Ambulance> getFleet() { return fleet; }
    public Queue<EmergencyRequest> getWaitingQueue() { return waitingQueue; }
    public List<EmergencyRequest> getHistory() { return history; }

    private boolean isSuitable(AmbulanceType ambType, EmergencyLevel level) {
        if (level == EmergencyLevel.CRITICAL) {
            return ambType == AmbulanceType.ICU || ambType == AmbulanceType.ALS;
        }
        if (level == EmergencyLevel.HIGH) {
            return ambType == AmbulanceType.ALS || ambType == AmbulanceType.ICU
                    || ambType == AmbulanceType.BASIC;
        }
        return true;
    }

    public double allocateAmbulance(EmergencyRequest req)
            throws InvalidRequestException, NoAmbulanceAvailableException {

        if (req.patientId == null || req.patientId.trim().isEmpty()) {
            throw new InvalidRequestException("Patient ID cannot be empty");
        }
        if (req.distanceKm < 0) {
            throw new InvalidRequestException("Distance cannot be negative");
        }

        Ambulance best = null;
        for (Ambulance a : fleet) {
            if (a.isAvailable() && isSuitable(a.type, req.level)) {
                if (best == null) {
                    best = a;
                } else if (req.level == EmergencyLevel.CRITICAL
                        && a.type == AmbulanceType.ICU
                        && best.type != AmbulanceType.ICU) {
                    best = a;
                }
            }
        }

        if (best == null) {
            req.status = "WAITING";
            waitingQueue.offer(req);
            throw new NoAmbulanceAvailableException(
                    "No suitable ambulance available. Request added to waiting queue.");
        }

        best.state = AmbulanceState.DISPATCHED;
        req.assignedAmbulance = best;
        req.status = "ASSIGNED";
        history.add(req);

        return req.distanceKm * 2.0;   // ETA in minutes
    }

    public void updateAmbulanceState(String ambId, AmbulanceState newState) {
        for (Ambulance a : fleet) {
            if (a.id.equals(ambId)) {
                a.state = newState;
                if (newState == AmbulanceState.AVAILABLE) {
                    tryAllocateFromQueue();
                }
                break;
            }
        }
    }

    private void tryAllocateFromQueue() {
        if (waitingQueue.isEmpty()) return;
        EmergencyRequest next = waitingQueue.peek();
        try {
            allocateAmbulance(next);
            waitingQueue.poll();
        } catch (Exception ignored) { }
    }

    // ---------- Main ----------
    public static void main(String[] args) {
        App system = new App();

        system.addAmbulance(new Ambulance("AMB-101", AmbulanceType.BASIC, "Ravi"));
        system.addAmbulance(new Ambulance("AMB-202", AmbulanceType.ALS, "Suresh"));
        system.addAmbulance(new Ambulance("AMB-303", AmbulanceType.ICU, "Dr. Meena"));

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter Patient ID: ");
        String pid = sc.nextLine();

        System.out.print("Emergency Level (CRITICAL/HIGH/MODERATE/NORMAL): ");
        EmergencyLevel level = EmergencyLevel.valueOf(sc.nextLine().trim().toUpperCase());

        System.out.print("Pickup Location: ");
        String pickup = sc.nextLine();

        System.out.print("Destination Hospital: ");
        String hospital = sc.nextLine();

        System.out.print("Estimated Distance (km): ");
        double dist = sc.nextDouble();

        EmergencyRequest req = new EmergencyRequest(pid, level, pickup, hospital, dist);

        try {
            double eta = system.allocateAmbulance(req);
            System.out.println("\n--- Dispatch Confirmation ---");
            System.out.println("Patient ID        : " + req.patientId);
            System.out.println("Emergency Level   : " + req.level);
            System.out.println("Assigned Ambulance: " + req.assignedAmbulance.id
                    + " (" + req.assignedAmbulance.type + ")");
            System.out.println("Driver            : " + req.assignedAmbulance.driverName);
            System.out.println("Status            : " + req.status);
            System.out.println("Estimated ETA     : " + eta + " minutes");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        sc.close();
    }
}
