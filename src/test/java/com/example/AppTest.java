package com.example;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import com.example.App.*;

public class AppTest {

    private App system;

    @Before
    public void setUp() {
        system = new App();
        system.addAmbulance(new Ambulance("AMB-101", AmbulanceType.BASIC, "Ravi"));
        system.addAmbulance(new Ambulance("AMB-202", AmbulanceType.ALS, "Suresh"));
        system.addAmbulance(new Ambulance("AMB-303", AmbulanceType.ICU, "Dr. Meena"));
    }

    @Test
    public void testCriticalGetsICUOrALS() throws Exception {
        EmergencyRequest req = new EmergencyRequest("P001", EmergencyLevel.CRITICAL,
                "MG Road", "City Hospital", 5.0);
        double eta = system.allocateAmbulance(req);
        assertEquals("ASSIGNED", req.status);
        assertTrue(req.assignedAmbulance.type == AmbulanceType.ICU
                || req.assignedAmbulance.type == AmbulanceType.ALS);
        assertEquals(10.0, eta, 0.01);
    }

    @Test
    public void testNormalCanTakeBasic() throws Exception {
        EmergencyRequest req = new EmergencyRequest("P002", EmergencyLevel.NORMAL,
                "Park Street", "General Hospital", 3.0);
        system.allocateAmbulance(req);
        assertEquals("ASSIGNED", req.status);
        assertNotNull(req.assignedAmbulance);
    }

    @Test
    public void testNoAmbulanceGoesToWaitingQueue() throws Exception {
        for (Ambulance a : system.getFleet()) {
            a.state = AmbulanceState.DISPATCHED;
        }
        EmergencyRequest req = new EmergencyRequest("P003", EmergencyLevel.HIGH,
                "Airport", "Trauma Center", 12.0);
        try {
            system.allocateAmbulance(req);
            fail("Should have thrown NoAmbulanceAvailableException");
        } catch (NoAmbulanceAvailableException e) {
            assertEquals("WAITING", req.status);
            assertEquals(1, system.getWaitingQueue().size());
        }
    }

    @Test(expected = InvalidRequestException.class)
    public void testEmptyPatientIdThrows() throws Exception {
        EmergencyRequest req = new EmergencyRequest("", EmergencyLevel.MODERATE,
                "Loc", "Hosp", 2.0);
        system.allocateAmbulance(req);
    }

    @Test(expected = InvalidRequestException.class)
    public void testNegativeDistanceThrows() throws Exception {
        EmergencyRequest req = new EmergencyRequest("P004", EmergencyLevel.NORMAL,
                "Loc", "Hosp", -5.0);
        system.allocateAmbulance(req);
    }

    @Test
    public void testAmbulanceStateTransition() throws Exception {
        EmergencyRequest req = new EmergencyRequest("P005", EmergencyLevel.NORMAL,
                "Station", "Clinic", 4.0);
        system.allocateAmbulance(req);
        assertEquals("ASSIGNED", req.status);

        system.updateAmbulanceState(req.assignedAmbulance.id, AmbulanceState.AVAILABLE);
        assertEquals(AmbulanceState.AVAILABLE, req.assignedAmbulance.state);
    }

    @Test
    public void testHistoryRecorded() throws Exception {
        EmergencyRequest req = new EmergencyRequest("P006", EmergencyLevel.HIGH,
                "Mall", "Hospital", 7.0);
        system.allocateAmbulance(req);
        assertEquals(1, system.getHistory().size());
        assertEquals("P006", system.getHistory().get(0).patientId);
    }
}
