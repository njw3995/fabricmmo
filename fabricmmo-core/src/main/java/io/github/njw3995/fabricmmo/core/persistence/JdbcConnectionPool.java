package io.github.njw3995.fabricmmo.core.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Small bounded JDBC pool with close-return semantics and validation on checkout. */
final class JdbcConnectionPool implements AutoCloseable {
    private final MySqlSettings settings;
    private final int maximum;
    private final ArrayDeque<Connection> idle = new ArrayDeque<>();
    private final Set<Connection> all = new HashSet<>();
    private boolean closed;

    JdbcConnectionPool(MySqlSettings settings) {
        this.settings = settings;
        this.maximum = settings.maxPoolSize();
    }

    synchronized Connection borrow() throws SQLException {
        while (true) {
            if (closed) throw new SQLException("FabricMMO JDBC pool is closed");
            while (!idle.isEmpty()) {
                Connection connection = idle.removeFirst();
                if (usable(connection)) return wrapper(connection);
                discard(connection);
            }
            if (all.size() < maximum) {
                Connection connection = DriverManager.getConnection(
                        settings.jdbcUrl(), settings.username(), settings.password());
                all.add(connection);
                return wrapper(connection);
            }
            try {
                wait(5_000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted waiting for a FabricMMO JDBC connection", exception);
            }
        }
    }

    private boolean usable(Connection connection) {
        try {
            return !connection.isClosed() && connection.isValid(2);
        } catch (SQLException ignored) {
            return false;
        }
    }

    private Connection wrapper(Connection physical) {
        InvocationHandler handler = new InvocationHandler() {
            private boolean returned;
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("close") && method.getParameterCount() == 0) {
                    if (!returned) {
                        returned = true;
                        release(physical);
                    }
                    return null;
                }
                if (method.getName().equals("isClosed") && method.getParameterCount() == 0) {
                    return returned || physical.isClosed();
                }
                if (returned) throw new SQLException("Pooled connection has already been returned");
                try {
                    return method.invoke(physical, args);
                } catch (java.lang.reflect.InvocationTargetException exception) {
                    throw exception.getCause();
                }
            }
        };
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, handler);
    }

    private synchronized void release(Connection connection) {
        if (!closed && usable(connection)) {
            try {
                if (!connection.getAutoCommit()) {
                    connection.rollback();
                    connection.setAutoCommit(true);
                }
                if (connection.isReadOnly()) connection.setReadOnly(false);
                connection.clearWarnings();
                idle.addLast(connection);
            } catch (SQLException exception) {
                discard(connection);
            }
        } else {
            discard(connection);
        }
        notifyAll();
    }

    private void discard(Connection connection) {
        all.remove(connection);
        idle.remove(connection);
        try { connection.close(); } catch (SQLException ignored) { }
    }

    @Override
    public synchronized void close() {
        closed = true;
        for (Connection connection : Set.copyOf(all)) discard(connection);
        notifyAll();
    }

    synchronized int totalConnections() { return all.size(); }
    synchronized int idleConnections() { return idle.size(); }
}
