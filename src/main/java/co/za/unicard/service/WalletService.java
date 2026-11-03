package co.za.unicard.service;

import co.za.unicard.exception.BusinessRuleException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.Learner;
import co.za.unicard.model.TxType;
import co.za.unicard.model.UserAccount;
import co.za.unicard.model.WalletTransaction;
import co.za.unicard.repository.UserAccountRepository;
import co.za.unicard.repository.WalletTransactionRepository;
import co.za.unicard.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * The only place balances change. Every debit checks funds and every movement is recorded
 * as a {@link WalletTransaction}. Callers must already be in a transaction.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class WalletService {

    private final WalletTransactionRepository txs;
    private final UserAccountRepository accounts;
    private final SchoolCalendar calendar;

    public WalletService(WalletTransactionRepository txs, UserAccountRepository accounts, SchoolCalendar calendar) {
        this.txs = txs;
        this.accounts = accounts;
        this.calendar = calendar;
    }

    /** Re-loads an account inside the current transaction so balance changes are saved. */
    public UserAccount managed(Long accountId) {
        return accounts.findById(accountId).orElseThrow(() -> new NotFoundException("Account", accountId));
    }

    public void debitAccount(UserAccount a, BigDecimal amount, TxType type, String description, String insufficientMessage) {
        if (Money.lessThan(a.getWalletBalance(), amount)) {
            throw new BusinessRuleException(insufficientMessage != null ? insufficientMessage
                    : "Your wallet has " + Money.format(a.getWalletBalance()) + ". Add money or choose another way to pay.");
        }
        a.debit(amount);
        txs.save(WalletTransaction.forAccount(a, type, description, amount.negate(), calendar.now()));
    }

    public void creditAccount(UserAccount a, BigDecimal amount, TxType type, String description) {
        a.credit(amount);
        txs.save(WalletTransaction.forAccount(a, type, description, amount, calendar.now()));
    }

    public void debitLearner(Learner l, BigDecimal amount, TxType type, String description) {
        if (Money.lessThan(l.getWalletBalance(), amount)) {
            throw new BusinessRuleException(l.getFirstName() + "'s wallet has " + Money.format(l.getWalletBalance()) + ". Move money first.");
        }
        l.debit(amount);
        txs.save(WalletTransaction.forLearner(l, type, description, amount.negate(), calendar.now()));
    }

    public void creditLearner(Learner l, BigDecimal amount, TxType type, String description) {
        l.credit(amount);
        txs.save(WalletTransaction.forLearner(l, type, description, amount, calendar.now()));
    }

    /** A zero-amount line in the learner's activity, e.g. "Collected at tuckshop". */
    public void noteLearner(Learner l, TxType type, String description) {
        txs.save(WalletTransaction.forLearner(l, type, description, Money.ZERO, calendar.now()));
    }
}
