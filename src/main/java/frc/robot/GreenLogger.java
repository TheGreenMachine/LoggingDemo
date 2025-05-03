package frc.robot;

import edu.wpi.first.util.datalog.*;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DriverStation;

import java.util.HashMap;
import java.util.function.Supplier;

public class GreenLogger {

    static {
        // this will log the robot modes i.e auto enabled estop
        DriverStation.startDataLog(DataLogManager.getLog(), false);
    }

    private static final HashMap<DataLogEntry,Supplier> periodicLogs = new HashMap<>();

    public static <T> void periodicLog(String name, Supplier<T> supplier){
        if(!Constants.kLoggingRobot) return;
        T result = supplier.get();
        DataLog log = DataLogManager.getLog();
        DataLogEntry dle = null;
        if(result instanceof Double){
            dle = new DoubleLogEntry(log, name);
        } else if ( result instanceof Integer) {
            dle = new IntegerLogEntry(log, name);
        } else if ( result instanceof Boolean) {
            dle = new BooleanLogEntry(log, name);
        } else {
            dle = new StringLogEntry(log, name);
        }
        if(dle != null)
            periodicLogs.put(dle,supplier);
    }

    public static void log(Object s) {
        if (Constants.kLoggingRobot) {
            DataLogManager.log(String.valueOf(s));
        }
    }


    public static void updatePeriodic() {
        for (DataLogEntry entry : periodicLogs.keySet()) {
            var supplier = periodicLogs.get(entry);
            if(entry instanceof DoubleLogEntry){
                ((DoubleLogEntry)entry).append((Double) supplier.get());
            } else if(entry instanceof IntegerLogEntry){
                ((IntegerLogEntry)entry).append((Integer) supplier.get());
            } else if(entry instanceof BooleanLogEntry){
                ((BooleanLogEntry)entry).append((Boolean) supplier.get());
            } else{
                ((StringLogEntry)entry).append(String.valueOf(supplier.get()));
            }
        }
    }
}
