package com.springapp.mvc.model.cloud;

import com.springapp.mvc.controller.Resources;
import com.springapp.mvc.model.abc.ArtificialBeeColony;
import com.springapp.mvc.model.abc.FoodSource;
import com.springapp.mvc.model.abc.Nectar;
import com.springapp.mvc.model.statistics.Statistics;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.io.PrintWriter;
import java.util.*;

/**
 * Created by Daniel on 3/12/2015.
 * FederationOfDataCenter class implements the cloud infrastructure
 */
public class FederationOfDataCenter extends SimEntity {

    public static final int PERIODIC_EVENT = 67567;
    public static final int DC_NUMBER = 67568;
    public static final int POWER_DATACENTER = 67569;
    public static final int DELAY = 300;
    //    public static final int TIME_STOP = 43200;
    public static final int TIME_STOP = 9900;
    private static final int V_IN = 3; //starting speed of energy production m/s
    private static final int V_OUT = 25; // finishing speed of energy production m/s
    private static final int PR = 225000; //windmill power w
    private static final int VR = 13; // speed for optimal production m/s
    public static double allocatedDC;
    private List<GreenDataCenter> dataCenterList;
    private List<GreenHost> hostList;
    private List<GreenVm> vmList;
    private List<Cloudlet> cloudletList;
    private Map<String, List<Double>> windSpeedMap;
    private DatacenterBroker broker;
    private PrintWriter fileWriter;

    public FederationOfDataCenter(String name) {
        super(name);
        try {
            fileWriter = new PrintWriter("energies.txt", "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startEntity() {
        send(getId(), 100, DC_NUMBER, null);
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev == null) {
            Log.printLine("Warning: " + CloudSim.clock() + ": " + this.getName() + ": Null event ignored.");
        } else {
            int tag = ev.getTag();
            switch (tag) {
                case PERIODIC_EVENT:
                    processPeriodicEvent();
                    break;
                case DC_NUMBER:
                    computeDCNumber();
                    break;
                case POWER_DATACENTER:
                    computeDataCenterPower();
                    break;
                default:
                    Log.printLine("Warning: " + CloudSim.clock() + ":" + this.getName() + ": Unknown event ignored. Tag:" + tag);
            }
        }

    }

    @Override
    public void shutdownEntity() {
        System.out.println();
    }

    private void computeDCNumber() {
        allocatedDC = getAllocatedDCFactor();
        double nextTimeStamp = allocatedDC + 200;
        send(getId(), nextTimeStamp, POWER_DATACENTER, new Object());
    }

    private void computeDataCenterPower() {
        double clock = CloudSim.clock();
        if (clock <= TIME_STOP + allocatedDC) {
            computeGreenPower(clock);
            if (clock >= 600 + allocatedDC) {
                initStatistics(clock);
                sendNow(getId(), PERIODIC_EVENT, new Object());
            } else {
                send(getId(), DELAY, POWER_DATACENTER, new Object());
            }
        }
    }

    private void processPeriodicEvent() {
        double clock = CloudSim.clock();
        runMigrationAlgorithm();
        boolean generatePeriodicEvent = true; //true if new internal events have to be generated
        if (clock >= TIME_STOP + allocatedDC) {
            generatePeriodicEvent = false;
            fileWriter.close();
            Statistics.printResults(dataCenterList);
        }
        if (generatePeriodicEvent) send(getId(), DELAY, POWER_DATACENTER, new Object());
    }

    public double getAllocatedDCFactor() {
        double counter = 0;
        for (GreenDataCenter dc : getDataCenterList()) {
            if (!dc.getVmList().isEmpty()) {
                counter++;
            }
        }
        return counter / 10;
    }


    public void computeGreenPower(double clock) {
        double energy;
        fileWriter.println(CloudSim.clock());
        for (GreenDataCenter dc : dataCenterList) {
            List<Double> windSpeedList = windSpeedMap.get(dc.getName());
            double windSpeed = windSpeedList.get((int) (clock - getAllocatedDCFactor()) / 300);
            energy = getWindEnergy(windSpeed);
            fileWriter.print(dc.getId() + " ");
            fileWriter.print(dc.getGreenEnergyQuantity() + " ");

            if (dc.getName().equals("DataCenter_0")) {
                dc.setGreenEnergyQuantity(Resources.SCHEDULING_INTERVAL * energy);
            }else{
                dc.setGreenEnergyQuantity(0.1);
            }

            fileWriter.println(dc.getGreenEnergyQuantity() + " ");
        }
    }

    private void initStatistics(Double currentTime) {
        for (GreenDataCenter dc : dataCenterList) {
            List<Double> values = new ArrayList<Double>(Collections.nCopies(9, 0.0));
            dc.getStatistics().put(currentTime, values);
            dc.setBrownEnergyQuantity(0);
            dc.setPower(0);
            dc.setCoolingEnergy(0);
            dc.setHeatGained(0);
            dc.setTotalEnergy(0);
            dc.setMigratingInVms(0);
            dc.setMigratingOutVms(0);
        }
    }

    private double getWindEnergy(double windSpeed) {
        double energy;
        if (windSpeed < V_IN || windSpeed > V_OUT) {
            energy = 0.1;
        } else if (windSpeed > VR && windSpeed < V_OUT) {
            energy = PR;
        } else {
            energy = (PR * (windSpeed - V_IN)) / (VR - V_IN);
        }
        return energy;
    }

    private void runMigrationAlgorithm() {

        List<Vm> greenVmList = getGreenVmList();
        Set<GreenVm> migratingSet = getMigrationVms(greenVmList);
        List<GreenVm> migratingList = new ArrayList<GreenVm>();
        if (migratingSet.size() != 0) {
            migratingList.addAll(migratingSet);
        }
        List<GreenDataCenter> DCList = getDataCenterList();
        ArtificialBeeColony abc = new ArtificialBeeColony(DCList, migratingList);
        FoodSource result = abc.runAlgorithm();
        scheduleMigrations(result);
        Statistics.analizeSolution(dataCenterList, result);
    }

    private Set<GreenVm> getMigrationVms(List<Vm> greenVmList) {

        GreenDataCenter greenDataCenter = dataCenterList.get(0);
        List<GreenVm> migratingVms = new ArrayList<GreenVm>();
        int vmNr;
        Random rand = new Random();
        if (greenDataCenter.getTotalEnergy() <= greenDataCenter.getGreenEnergyQuantity()) {
            for (Vm vm : greenVmList) {
                GreenDataCenter dc = (GreenDataCenter) vm.getHost().getDatacenter();
                if (dc.getGreenEnergyQuantity() < 0.5) {
                    migratingVms.add((GreenVm) vm);
                }
            }
            int low = (int) (migratingVms.size() * 0.25);
            int high = (int) (migratingVms.size() * 0.5);
            vmNr = rand.nextInt(high - low) + low;
        } else {
            for (Host h : greenDataCenter.getHostList()) {
                migratingVms.addAll(h.<GreenVm>getVmList());
            }
            double energyRatio = greenDataCenter.getGreenEnergyQuantity() / greenDataCenter.getTotalEnergy();
            vmNr = (int) ((1 - energyRatio) * migratingVms.size()) + 1;
        }
        Set<GreenVm> migratingSet = new HashSet<GreenVm>();
        while (migratingSet.size() < vmNr) {
            int index = rand.nextInt(migratingVms.size());
            GreenVm vm = migratingVms.get(index);
            Host host = vm.getHost();
            if (host != null) {
                migratingSet.add(vm);
            }

        }
        return migratingSet;
    }

    private List<Vm> getGreenVmList() {
        List<Vm> greenVmList = new ArrayList<Vm>();
        for (GreenDataCenter dc : dataCenterList) {
            List<GreenHost> greenHosts = dc.getHostList();
            for (GreenHost h : greenHosts) {
                greenVmList.addAll(h.getVmList());
            }
        }
        return greenVmList;
    }

    private void scheduleMigrations(FoodSource result) {
        for (Nectar n : result.getNectarList()) {
            Host host = n.getHost();
            Vm vm = n.getVm();
            GreenDataCenter dataCenter = (GreenDataCenter) vm.getHost().getDatacenter();
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("vm", vm);
            data.put("host", host);
            sendNow(dataCenter.getId(), CloudSimTags.VM_MIGRATE, data);
        }
    }


    public DatacenterBroker getBroker() {
        return broker;
    }

    public void setBroker(DatacenterBroker broker) {
        this.broker = broker;
    }

    public List<GreenDataCenter> getDataCenterList() {
        return dataCenterList;
    }

    public void setDataCenterList(List<GreenDataCenter> dataCenterList) {
        this.dataCenterList = dataCenterList;
    }

    public List<GreenHost> getHostList() {
        return hostList;
    }

    public void setHostList(List<GreenHost> hostList) {
        this.hostList = hostList;
    }

    public List<GreenVm> getVmList() {
        return vmList;
    }

    public void setVmList(List<GreenVm> vmList) {
        this.vmList = vmList;
    }

    public List<Cloudlet> getCloudletList() {
        return cloudletList;
    }

    public void setCloudletList(List<Cloudlet> cloudletList) {
        this.cloudletList = cloudletList;
    }

    public void setWindSpeedMap(Map<String, List<Double>> windSpeedMap) {
        this.windSpeedMap = windSpeedMap;
    }
}
