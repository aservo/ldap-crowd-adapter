package test.api;


public interface IntegrationTestBackendSetup
        extends BackendSetup {

    default void startup()
            throws Exception {
    }

    default void shutdown()
            throws Exception {
    }
}
