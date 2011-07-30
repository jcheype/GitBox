package com.jcheype.gitbox;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

/**
 * @author Jean-Baptiste Lemée
 */
public class GitBoxCredentialsProvider extends CredentialsProvider {
    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public boolean supports(CredentialItem... items) {
        return true;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        for (CredentialItem credentialItem : items) {
            if (credentialItem instanceof CredentialItem.YesNoType) {
                ((CredentialItem.YesNoType) credentialItem).setValue(true);
            } else if (credentialItem instanceof CredentialItem.Password) {
                // Prompt password here ?
                //((CredentialItem.Password) credentialItem).setValue("");
            } else if (credentialItem instanceof CredentialItem.StringType) {
                // Any message could be prompt and answer by the user here (like the passphrase)
                //((CredentialItem.StringType) credentialItem).setValue("The answer");
            }
        }
        return true;
    }
}
