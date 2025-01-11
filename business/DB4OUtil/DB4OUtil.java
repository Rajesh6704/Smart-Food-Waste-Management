package business.DB4OUtil;

import business.ConfigureEcoSystem;
import business.EcoSystem;
import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.config.EmbeddedConfiguration;
import com.db4o.query.Predicate;
import com.db4o.ta.TransparentPersistenceSupport;

/**
 *
 * @author rrheg
 */
public class DB4OUtil {

    private static final String FILENAME = "DataBank.db4o"; // Path to the data store
    private static DB4OUtil dB4OUtil;

    // Singleton pattern to ensure only one instance of DB4OUtil exists
    public synchronized static DB4OUtil getInstance() {
        if (dB4OUtil == null) {
            dB4OUtil = new DB4OUtil();
        }
        return dB4OUtil;
    }

    // Closes the database connection
    protected synchronized static void shutdown(ObjectContainer conn) {
        if (conn != null) {
            conn.close();
        }
    }

    // Creates a connection to the database
    private ObjectContainer createConnection() {
        try {
            EmbeddedConfiguration config = Db4oEmbedded.newConfiguration();
            config.common().add(new TransparentPersistenceSupport());

            // Controls the number of objects in memory
            config.common().activationDepth(Integer.MAX_VALUE);

            // Controls the depth/level of updating the object
            config.common().updateDepth(Integer.MAX_VALUE);

            // Register your top-most class here
            config.common().objectClass(EcoSystem.class).cascadeOnUpdate(true); // Enable cascading updates for EcoSystem

            // Open the database file
            return Db4oEmbedded.openFile(config, FILENAME);
        } catch (Exception ex) {
            System.err.println("Error while creating database connection: " + ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    // Stores the system object into the database
    public synchronized void storeSystem(EcoSystem system) {
        ObjectContainer conn = createConnection();
        if (conn == null) {
            System.err.println("Database connection could not be established. System not saved.");
            return;
        }
        try {
            deleteOldSystem(conn); // Deletes the old system before saving a new one
            conn.store(system);    // Store the new system
            conn.commit();         // Commit the changes
        } catch (Exception ex) {
            System.err.println("Error while storing the system: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            conn.close();
        }
    }

    // Deletes any old system objects from the database
    private void deleteOldSystem(ObjectContainer conn) {
        try {
            ObjectSet<EcoSystem> systems = conn.query(new Predicate<EcoSystem>() {
                @Override
                public boolean match(EcoSystem et) {
                    return true; // Matches all EcoSystem objects
                }
            });

            for (EcoSystem ecoSystem : systems) {
                conn.delete(ecoSystem);
            }
        } catch (Exception ex) {
            System.err.println("Error while deleting old systems: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Retrieves the system object from the database
    public EcoSystem retrieveSystem() {
        ObjectContainer conn = createConnection();
        if (conn == null) {
            System.err.println("Database connection could not be established. Returning a new system.");
            return ConfigureEcoSystem.configure(); // Return a new system if no connection could be established
        }

        EcoSystem system = null;
        try {
            ObjectSet<EcoSystem> systems = conn.query(EcoSystem.class); // Query for all EcoSystem objects
            if (systems.isEmpty()) {
                // If no system exists, create a new one
                system = ConfigureEcoSystem.configure();
            } else {
                // Retrieve the last saved system
                system = systems.get(systems.size() - 1);
            }
        } catch (Exception ex) {
            System.err.println("Error while retrieving the system: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            conn.close();
        }
        return system;
    }
}
