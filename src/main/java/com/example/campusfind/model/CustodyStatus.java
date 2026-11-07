package com.example.campusfind.model;

/**
 * Indicates physical possession and custody location of a found campus item.
 */
public enum CustodyStatus {
    WITH_FINDER("In Finder's Possession", "The reporting student or staff member currently has the item in hand."),
    SECURITY_DESK("Campus Security Office (Gate 1)", "Deposited at the 24/7 Campus Security Helpdesk at Main Gate 1."),
    LIBRARY_DESK("Central Library Helpdesk (Floor 1)", "Turned in to the 1st Floor Central Library Circulation Counter."),
    DEPARTMENT_OFFICE("Department Admin Office", "Submitted to the departmental administrative front desk.");

    private final String label;
    private final String description;

    CustodyStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
}

