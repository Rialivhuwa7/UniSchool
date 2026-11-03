package co.za.unicard.service;

import co.za.unicard.model.Learner;
import co.za.unicard.model.TxType;
import co.za.unicard.model.UserAccount;
import co.za.unicard.repository.WalletTransactionRepository;
import co.za.unicard.security.Actor;
import co.za.unicard.util.Money;
import co.za.unicard.dto.LearnerDtos.TxView;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ParentWalletService {

    public record WalletView(BigDecimal balance) {}

    private final WalletService wallets;
    private final WalletTransactionRepository txs;
    private final LearnerAccess access;

    public ParentWalletService(WalletService wallets, WalletTransactionRepository txs, LearnerAccess access) {
        this.wallets = wallets;
        this.txs = txs;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public WalletView wallet(Actor actor) {
        return new WalletView(wallets.managed(actor.accountId()).getWalletBalance());
    }

    /**
     * Adds money to the parent's wallet. In production this is called after the card or EFT
     * payment provider confirms the payment; here it credits directly.
     */
    public WalletView topUp(Actor actor, BigDecimal amount) {
        UserAccount a = wallets.managed(actor.accountId());
        wallets.creditAccount(a, Money.round(amount), TxType.TOP_UP, "Wallet top-up");
        return new WalletView(a.getWalletBalance());
    }

    /** Parent wallet to a child's card wallet. */
    public WalletView moveToChild(Actor actor, Long learnerId, BigDecimal amount) {
        UserAccount a = wallets.managed(actor.accountId());
        Learner l = access.requireAsParent(actor, learnerId);
        BigDecimal amt = Money.round(amount);
        wallets.debitAccount(a, amt, TxType.TRANSFER, "Moved to " + l.getFirstName() + "'s wallet",
                "Not enough in your wallet. You have " + Money.format(a.getWalletBalance()) + ".");
        wallets.creditLearner(l, amt, TxType.TRANSFER, "From " + a.getFirstName());
        return new WalletView(a.getWalletBalance());
    }

    /** The parent's own transactions plus all their children's. */
    @Transactional(readOnly = true)
    public List<TxView> activity(Actor actor) {
        List<Long> kids = access.myLearners(actor).stream().map(Learner::getId).toList();
        List<Long> ids = kids.isEmpty() ? List.of(-1L) : kids;
        return txs.activity(actor.accountId(), ids, PageRequest.of(0, 30)).stream().map(TxView::of).toList();
    }
}
