package org.messageduct.account.messages;

/**
 * A marker interface for AccountMessages that do not require the user to be currently logged in.
 * Any other account messages will fail if the user is not logged in.
 */
public interface NonAuthenticatedAccountMessage extends AccountMessage {
}
