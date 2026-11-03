package co.za.unicard.security;

import co.za.unicard.model.Role;
import co.za.unicard.model.RoleAssignment;
import co.za.unicard.model.UserAccount;


/** The signed-in account and the role they are currently acting in. */
public record Actor(UserAccount account, RoleAssignment roleAssignment) {
    public Role role() { return roleAssignment.getRole(); }
    public Long accountId() { return account.getId(); }
    public boolean is(Role r) { return role() == r; }
}
