package org.apache.cayenne.conn.support;

import javax.sql.DataSource;

import org.apache.cayenne.tx.support.TransactionSynchronizationSupport;
import org.apache.cayenne.tx.support.TransactionSynchronizerAdapter;

/**
 * Callback for resource cleanup at the end of a non-native JDBC transaction
 * (e.g. when participating in a JtaTransactionManager transaction).
 * 
 * @author devacfr<christophefriederich@mac.com>
 * @since 3.2
 */
class ConnectionSynchronization extends TransactionSynchronizerAdapter {

    /**
     * 
     */
    public static final int CONNECTION_SYNCHRONIZATION_ORDER = 1000;

    
    /**
     * 
     */
    private final ConnectionHolder connectionHolder;

    /**
     * 
     */
    private final DataSource dataSource;


    /**
     * 
     */
    private boolean holderActive = true;

    public ConnectionSynchronization(final ConnectionHolder connectionHolder, final DataSource dataSource) {
        this.connectionHolder = connectionHolder;
        this.dataSource = dataSource;
    }


    @Override
    public void suspend() {
        if (this.holderActive) {
            TransactionSynchronizationSupport.unbindResource(this.dataSource);
            if (this.connectionHolder.hasConnection() && !this.connectionHolder.isOpen()) {
                DataSources.releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
                this.connectionHolder.setConnection(null);
            }
        }
    }

    @Override
    public void resume() {
        if (this.holderActive) {
            TransactionSynchronizationSupport.bindResource(this.dataSource, this.connectionHolder);
        }
    }

    @Override
    public void beforeCompletion() {

        if (!this.connectionHolder.isOpen()) {
            TransactionSynchronizationSupport.unbindResource(this.dataSource);
            this.holderActive = false;
            if (this.connectionHolder.hasConnection()) {
                DataSources.releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
            }
        }
    }

    @Override
    public void afterCompletion(final Status status) {

        if (this.holderActive) {
            TransactionSynchronizationSupport.unbindResourceIfPossible(this.dataSource);
            this.holderActive = false;
            if (this.connectionHolder.hasConnection()) {
                DataSources.releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
                this.connectionHolder.setConnection(null);
            }
        }
        this.connectionHolder.reset();
    }
    

}