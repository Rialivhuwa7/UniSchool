package co.za.unicard.model;

public enum LedgerSource {
    COLLECTION,        // trips, events, levies, tickets, donations
    FEES,              // school fee debit orders
    SHOP_GIVEBACK,     // 5% of UniSchool shop orders
    UNIFORM_HANDLING   // handling fee on uniform shop sales
}
