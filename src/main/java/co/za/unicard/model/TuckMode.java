package co.za.unicard.model;

/** What a learner may get at the tuckshop, chosen by the parent. */
public enum TuckMode {
    /** Only what a parent pre-orders. They tap their card to collect; anything else is declined. */
    PREORDER_ONLY,
    /** Pre-orders plus items the parent approves, up to the daily limit. */
    APPROVED_ITEMS,
    /** Anything within the daily limit, except blocked categories. */
    FREE_CHOICE
}
