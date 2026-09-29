package io.openliberty.sample.jakarta.ejb.session_synchronization_method;

import jakarta.ejb.AfterBegin;
import jakarta.ejb.BeforeCompletion;
import jakarta.ejb.AfterCompletion;

/**
 * Plain class (no session-bean annotation) with otherwise-invalid session
 * synchronization methods.  No diagnostics should be reported because the
 * session-sync rules only apply to session beans.
 */
public class NonSessionBeanWithSyncAnnotations {

    // Would be invalid on a session bean, but this class is not a session bean
    @AfterBegin
    public final void beginSync() {
    }

    @BeforeCompletion
    public static void beforeCommit() {
    }

    @AfterCompletion
    public int afterComplete(boolean committed) {
        return 0;
    }
}
