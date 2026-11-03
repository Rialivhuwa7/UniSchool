package co.za.unicard.model;

public enum TxType {
    TOP_UP,
    TRANSFER,
    TUCKSHOP_SALE,
    TUCKSHOP_PREORDER,
    TUCKSHOP_REFUND,
    TUCKSHOP_COLLECTED,   // zero-amount record that a pre-order was handed over
    SHOP_ORDER,
    SCHOOL_PAYMENT,
    SAVINGS,
    POCKET_MONEY,
    GOAL_SAVING
}
